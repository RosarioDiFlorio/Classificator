package eu.innovation.engineering.LSA.keywordExtractor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.util.CleanUtilis;
import eu.innovationengineering.lang.ISO_639_1_LanguageCode;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;
import eu.innovationengineering.nlp.beans.AnnotatedWord;
import eu.innovationengineering.nlp.beans.CompleteSentenceAnalysis;
import eu.innovationengineering.nlp.beans.oat.SentenceChunk;

/**
 * @author Rosario
 * @author Luigi
 *
 */
public class LSAKeywordExtractor implements KeywordExtractor {  
  /* (non-Javadoc)
   * @see eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor#extractKeywordsFromText(java.util.List, int)
   */
  @Override
  public  List<Keyword> extractKeywordsFromText(List<String> toAnalyze, int numKeywordsToReturn) throws Exception {
    List<Keyword> keywordList = new ArrayList<Keyword>();
    for(String text: toAnalyze){
      List<List<String>> sentenceList = createSentencesFromText(text);
      MatrixRepresentation matrixA = buildMatrixA(sentenceList);
      System.out.println(matrixA.getMatrixA().toString());
      Array2DRowRealMatrix U = SVD(matrixA);
      keywordList = getKeywordList(matrixA, U, numKeywordsToReturn);
    }
    return keywordList;

  }

  /**
   * @param text
   * @return
   * @throws LanguageException 
   */
  public static List<SentenceChunk> createChunkFromText(String text) throws LanguageException{
    text = text.toLowerCase();
    String[] cleanedString = text.split(" ");
    text = "";
    for(String s : cleanedString){
      if(!CleanUtilis.getBlackList().contains(s))
        text+= s+" ";
    }
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();
    CompleteSentenceAnalysis results = nlpAnalyzer.executeCompleteSentenceAnalysis(text, ISO_639_1_LanguageCode.ENGLISH, true, true);
    List<SentenceChunk> chunkList = cleanChunks(results.getChunks());

    //debug print
    //List<List<String>> toprint = chunkList.stream().map(sc->sc.getWords().stream().map(w->w.getWord()).collect(Collectors.toList())).collect(Collectors.toList());
    //toprint.stream().forEach(System.out::println);


    return chunkList;
  }

  /**
   * @param chunks
   * @return
   */
  private static List<SentenceChunk> cleanChunks(List<SentenceChunk> chunks){
    Set<String> stopwords = CleanUtilis.getBlackList();

    for(Iterator<SentenceChunk> it = chunks.iterator();it.hasNext();){
      SentenceChunk sc = it.next();
      Iterator<AnnotatedWord> iter = sc.getWords().iterator();
      while(iter.hasNext()){
        AnnotatedWord word = iter.next();
        if(stopwords.contains(word.getWord()) && word.getWord().length()<=2)
          iter.remove();
      }
      if(sc.getWords().isEmpty())
        it.remove();
      //System.out.println(sc.getWords().stream().map(an->an.getWord()).collect(Collectors.toList()).toString());
    }
    return chunks;
  }

  /**
   * Create matrix A from chuncks
   * @param chunks
   * @return
   */
  public static MatrixRepresentation buildMatrixA(List<List<String>> sentences){

    List<String> wordList = new ArrayList<String>();



    //crea la lista di word
    for(List<String> sentencce : sentences){
      for(String word : sentencce){
        if(!wordList.contains(word)){
          wordList.add(word);
        }
      }
    }

    //crea la matrice
    Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(wordList.size(),sentences.size());
    int row=0;
    int column=0;

    for(String word : wordList){
      column=0;
      for(List<String> sentence : sentences){    
        matrix.addToEntry(row, column, Tf(word, sentences, column)*Isf(word,sentences));
        column++;
      }
      row++;
    }

    MatrixRepresentation matrixA = new MatrixRepresentation();
    matrixA.setMatrixA(matrix);
    matrixA.setTokenList(wordList);

    return matrixA;
  }

  /**
   * return matrix U after SVD decomposition
   * @param <E>
   * @return toDefine
   */
  public static Array2DRowRealMatrix SVD(MatrixRepresentation matrixA){  

    SingularValueDecomposition svd = new SingularValueDecomposition(matrixA.getMatrixA());


    return (Array2DRowRealMatrix) svd.getU();
  }




  /**
   * Return keywordList from matrix U after SVD decomposition
   * @param matrixA
   * @param SVDResult
   * @return
   */
  private static  List<Keyword> getKeywordList(MatrixRepresentation matrixA, Array2DRowRealMatrix U, int threshold){

    double[] bestColumn = U.getColumn(0);

    HashMap<Integer,Double> bestIndex = new HashMap<Integer,Double>();
    List<Keyword> keywordList = new ArrayList<Keyword>();

    if(threshold<=bestColumn.length){
      while(threshold>0){
        int index= max(bestColumn);
        bestIndex.put(index,bestColumn[index]);
        bestColumn[index]=0;
        threshold--;
      }
    }
    else
    {
      System.out.println("Threshold value is greater then column size");
      return null;
    }

    for(int index : bestIndex.keySet()){
      Keyword k = new Keyword();
      k.setText(matrixA.getTokenList().get(index));
      k.setRelevance(bestIndex.get(index));
      keywordList.add(k);
    }

    return keywordList;
  }

  /**
   * return position of max value into double array 
   * @param bestColumn
   * @return
   */
  private static int max(double[] bestColumn) {
    double max = 0;
    int indexMax=0;
    for(int i=0; i<bestColumn.length;i++){
      if(bestColumn[i]>max){
        max = bestColumn[i];
        indexMax = i;
      }
    }

    return indexMax;


  }



  /**
   * return TF value
   * @param word
   * @param chunks
   * @param j
   * @return
   */
  private static float Tf(String word,List<List<String>> sentences, int j){

    List<String> sentenceJ = sentences.get(j);
    //number of times word i in sentence j
    int countSentenceJ=0;
    for(String tmpWord : sentenceJ){
      if(tmpWord.equals(word)){
        countSentenceJ++;
      }
    }
    
    return countSentenceJ/sentenceJ.size();

  }


  /**
   * return ISF value
   * @param word
   * @param chunks
   * @return
   */
  private static float Isf(String word,List<List<String>> sentences){

    //number of sentences with word i
    float numberSentenceWithWord = 0;

    for(List<String> sentence : sentences){
      if(sentence.contains(word))
        numberSentenceWithWord++;
    }

    return sentences.size()/numberSentenceWithWord;
  }


}

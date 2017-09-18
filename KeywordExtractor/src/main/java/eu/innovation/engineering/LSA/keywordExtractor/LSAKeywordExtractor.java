package eu.innovation.engineering.LSA.keywordExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;
import eu.innovationengineering.nlp.beans.AnnotatedWord;
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
  public List<Keyword> extractKeywordsFromText(List<String> toAnalyze, int numKeywordsToReturn) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @param text
   * @return
   */
  public static List<String> createChunkFromText(String text){
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();
    return null;
  }

  /**
   * @param chunks
   * @return
   */
  public static List<String> cleanChunks(List<String> chunks){
    return null;
  }

  /**
   * Create matrix A from chuncks
   * @param chunks
   * @return
   */
  public static MatrixRepresentation buildMatrixA(List<SentenceChunk> chunks){

    List<String> wordList = new ArrayList<String>();
    Array2DRowRealMatrix matrix = new Array2DRowRealMatrix();

    //crea la lista di word
    for(SentenceChunk chunk : chunks){
      for(AnnotatedWord word : chunk.getWords()){
        if(!wordList.contains(word.getWord())){
          wordList.add(word.getWord());
        }
      }
    }

    //crea la matrice
    int row=0;
    int column=0;

    for(String word : wordList){
      column=0;
      for(SentenceChunk chunk : chunks){
        matrix.addToEntry(row, column, Tf(word, chunks, column)*Isf(word,chunks));
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
  private static float Tf(String word,List<SentenceChunk> chunks, int j){
    
    SentenceChunk sentenceJ = chunks.get(j);
    //number of times word i in sentence j
    int countSentenceJ=0;
    for(AnnotatedWord tmpWord : sentenceJ.getWords())
      if(tmpWord.getWord().equals(word))
        countSentenceJ++;
     
    return countSentenceJ/sentenceJ.getWords().size();
  }

  /**
   * return ISF value
   * @param word
   * @param chunks
   * @return
   */
  private static float Isf(String word,List<SentenceChunk> chunks){
    
    //number of sentences with word i
    int numberSentenceWithWord = 0;
    
    for(SentenceChunk sentence : chunks){
      List<String> wordList = sentence.getWords().stream().map(AnnotatedWord::getWord).collect(Collectors.toList());
      if(wordList.contains(word))
        numberSentenceWithWord++;
    }
    
    
    return chunks.size()/numberSentenceWithWord;
  }


}

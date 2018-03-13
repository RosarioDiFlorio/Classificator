package eu.innovationengineering.extractor;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import eu.innovationengineering.lang.ISO_639_1_LanguageCode;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;
import eu.innovationengineering.persistence.SQLiteVectors;
import eu.innovationengineering.utilities.Lemmatizer;
import eu.innovationengineering.utilities.Similarities;
import eu.innovationengineering.utilities.StopWords;
import eu.innovationengineering.utilities.Word2Vec;

/**
 * @author Rosario
 * @author Luigi
 *
 */
public class LSACosineKeywordExtraction implements InitializingBean {  

  private List<String> toCompare;
  //  private Map<String,Set<float[]>> glossaryMap;
  private Map<String,float[]> glossaryMap;
  @Autowired
  private StopWords stopwords;
  @Autowired
  private Lemmatizer lemmatizer;

  @Autowired
  private StanfordnlpAnalyzer nlpAnalyzer;
  
  @Autowired
  private Word2Vec word2Vec;
  
  @Autowired
  @Qualifier("glossariesVectors")
  private SQLiteVectors glossariesDB;

  
  public LSACosineKeywordExtraction(){
    
  }

  public void initGlossary(){
    glossaryMap = glossariesDB.getAllVectors();
    if(glossaryMap.isEmpty()){
      glossaryMap.put("root", new float[1]);
    }
  }


  public List<List<Keyword>> extractKeywordsFromTexts(List<String> toAnalyze,Set<String> categories, int numKeywordsToReturn) throws Exception {
    List<List<Keyword>> toReturn = new ArrayList<List<Keyword>>();

    Set<float[]> vectors = new HashSet<>();
    if(categories.isEmpty())
      vectors.add(glossaryMap.get("root"));
    else{
      for(String category:categories){
        if(glossaryMap.containsKey(category))
          vectors.add(glossaryMap.get(category));
      }
    }

    for(String text: toAnalyze){
      List<Keyword> keywordList = new ArrayList<Keyword>();
      List<List<String>> sentenceList = createSentencesFromText(text);
      MatrixRepresentation matrixA = buildMatrixA(sentenceList,vectors);
      RealMatrix U = SVD(matrixA);
      keywordList = getKeywordList(matrixA, U, numKeywordsToReturn);
      toReturn.add(keywordList);
      matrixA = null;
      Runtime.getRuntime().gc();
    }
    return toReturn;

  }

  /**
   * Create the list of sentence for a document.
   * @param document - The string representing the document to split in sentence.
   * @return The list of sentence for the document, each list is a list of word of the sentence.
   * @throws LanguageException 
   */
  public  List<List<String>> createSentencesFromText(String document) throws LanguageException{
    List<List<String>> sentecesList = new ArrayList<List<String>>();
    List<String> senteces = nlpAnalyzer.detectSentences(document.toLowerCase(), ISO_639_1_LanguageCode.ENGLISH);
    /*List<String> senteces = new ArrayList<>();
    senteces.add(document.toLowerCase());*/
    for(String sentence: senteces)
    {
      sentecesList.add(cleanAndSplitSentence(sentence, lemmatizer));
    }

    return sentecesList;
  }

  /**
   * Trasform the sentence in lower case, clean the sentence from punctuation,
   * remove eventually stopwords, lemmatize each word of the sentence
   * and create a list of word.
   * @param sentence - The string representing one sentence.
   * @param lemmatizer 
   * @return The list of words for a sentence.
   */
  private  List<String> cleanAndSplitSentence(String sentence, Lemmatizer lemmatizer){
    //sentence = sentence.replaceAll("(\\.-/#\\^)", " ");
    StringBuilder strbuilder = new StringBuilder();
    stopwords.cleanText(sentence).forEach(s->strbuilder.append(s+" "));
    List<String> textLemmatized = lemmatizer.lemmatize(strbuilder.toString());
    Iterator<String> it = textLemmatized.iterator();

    while(it.hasNext()){
      String str = it.next();
      if(stopwords.isStopWord(str.toLowerCase()) || str.length()<=2)
        it.remove();
    }
    //    System.out.println(textLemmatized);
    return textLemmatized;
  }


  /**
   * Build the matrix used for the SVD method.
   * @param sentences - the sentence's list of the document.
   * @return A MatrixRepresentation object that contains a double[][] matrix and a list of unique words.
   * An matrix A (nm) is a matrix that has n words and m sentences that make up the document. 
   * Each cell in the matrix represents the weight that the term has in the corresponding sentence.
   * @throws IOException 
   */
  public  MatrixRepresentation buildMatrixA(List<List<String>> sentences,Set<float[]> toCompareVectors) throws IOException{
    List<String> wordList = new ArrayList<String>();


    //crea la lista di word
    List<List<String>> textList = new ArrayList<>();
    for(List<String> sentence : sentences){
      for(String word : sentence){
        if(!wordList.contains(word)){
          List<String> tmp = new ArrayList<>();
          wordList.add(word); 
          tmp.add(word);
          textList.add(tmp);
        }
      }
    }
    float[][] wordVectors = word2Vec.returnVectorsFromTextList(textList);

    //crea la matrice
    Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(wordList.size(),sentences.size());
    int row=0;
    int column=0;

    for(int i = 0;i<wordList.size();i++){
      column=0;
      String word = wordList.get(i);
      double weigth = 0;
      for(float[] vector:toCompareVectors){ 
        weigth += Similarities.cosineSimilarity(wordVectors[i], vector);
      }
      weigth /= sentences.size();
      for(List<String> sentence : sentences){     
        if(sentence.contains(word)){
          weigth += Tf(word, sentences, column);
        }else{
          weigth += 0;
        }
        //        System.out.println(word+" "+(weigth/sentences.size()));
        matrix.addToEntry(row, column,weigth);
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
   * Perform the SVD method and return U.
   *  U is an mxm RealMatrix
   * @param matrixA - The MatrixRepresentation object that contains the matrix for the 
   *        SVD decomposition.
   * @return matrix U after SVD decomposition
   */
  public  RealMatrix SVD(MatrixRepresentation matrixA){  

    SingularValueDecomposition svd = new SingularValueDecomposition(matrixA.getMatrixA());


    return svd.getU();
  }


  /**
   *  Return keywordList from matrix U after SVD decomposition
   * @param matrixA - The MatrixRepresentation object that contains the 
   * original matrix.
   * @param U - is the mxm matrix result of the SVD method.
   * @param threshold - number of keyword that the method have to consider.
   * @return The list of Keyword 
   */
  private List<Keyword> getKeywordList(MatrixRepresentation matrixA, RealMatrix U, int threshold){

    List<Keyword> keywordList = new ArrayList<Keyword>();


    double[] bestColumn = U.getColumn(0);
    HashMap<Integer,Double> bestIndex = new HashMap<Integer,Double>();
    
    if(threshold > bestColumn.length)
      threshold = bestColumn.length;

    if(threshold<=bestColumn.length){
      while(threshold>0){
        int index = max(bestColumn);
        bestIndex.put(index,bestColumn[index]);
        bestColumn[index]=-10000;
        threshold--;
      }
    }
    else
    {
      System.out.println("Threshold value is greater then column size");
      return keywordList;
    }
    for(int index : bestIndex.keySet()){
      Keyword k = new Keyword();
      k.setText(matrixA.getTokenList().get(index));
      //System.out.println(U.getEntry(index, 0)+" "+translateFunction(U.getEntry(index, 0)));
      k.setRelevance(translateFunction(U.getEntry(index, 0)));
      keywordList.add(k);
    }

    return keywordList;
  }   

  /**
   * return position of max value into double array 
   * @param bestColumn
   * @return
   */
  private  int max(double[] bestColumn) {
    double max = bestColumn[0];
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
  private  double Tf(String word,List<List<String>> sentences, int j){

    List<String> sentenceJ = sentences.get(j);
    //number of times word i in sentence j
    double countSentenceJ=0;
    for(String tmpWord : sentenceJ){
      if(tmpWord.equals(word)){
        countSentenceJ++;
      }
    }

    double results = countSentenceJ/sentenceJ.size();
    return results;

  }


  /**
   * return ISF value
   * @param word
   * @param sentences
   * @return
   */
  private  double Isf(String word,List<List<String>> sentences){

    //number of sentences with word i
    double numberSentenceWithWord = 0;

    for(List<String> sentence : sentences){
      if(sentence.contains(word))
        numberSentenceWithWord++;
    }

    return numberSentenceWithWord/sentences.size();
  }

  private double translateFunction(double x){
    //System.out.println(x);
    return (Math.atan(5 * x - 3)/Math.PI)+(0.5);
  }

  public List<String> getToCompare() {
    return toCompare;
  }


  public void setToCompare(List<String> toCompare) {
    this.toCompare = toCompare;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    initGlossary();
    
  }


}

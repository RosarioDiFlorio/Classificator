package eu.innovation.engineering.LSA.keywordExtractor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.util.CleanUtilis;
import eu.innovationengineering.lang.ISO_639_1_LanguageCode;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;

/**
 * @author Rosario
 * @author Luigi
 *
 */
public class LSAKeywordExtractor implements KeywordExtractor {  



  private String mainDirectory = "";
  private static String stopWordPath= "data/stopwords/stopwords_en.txt";



  public LSAKeywordExtractor(String mainDir) {
    setMainDirectory(mainDir);
    setStopWordPath(getMainDirectory() + stopWordPath);
  }


  /* (non-Javadoc)
   * @see eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor#extractKeywordsFromText(java.util.List, int)
   */
  @Override
  public  List<List<Keyword>> extractKeywordsFromTexts(List<String> toAnalyze, int numKeywordsToReturn) throws Exception {
    List<List<Keyword>> toReturn = new ArrayList<List<Keyword>>();
    for(String text: toAnalyze){
      List<Keyword> keywordList = new ArrayList<Keyword>();
      System.out.println("Senteces step start");
      List<List<String>> sentenceList = createSentencesFromText(text);
      System.out.println("Step over\n Next Step -> building matrix A");
      MatrixRepresentation matrixA = buildMatrixA(sentenceList);
      System.out.println("Step over\n Next Step -> SVD decomposition");
      RealMatrix U = SVD(matrixA);
      keywordList = getKeywordList(matrixA, U, numKeywordsToReturn);
      System.out.println("All step completed");
      toReturn.add(keywordList);
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
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();
    List<String> senteces = nlpAnalyzer.detectSentences(document, ISO_639_1_LanguageCode.ENGLISH);
    Lemmatizer lemmatizer = new Lemmatizer();
    for(String sentence: senteces){
      sentecesList.add(cleanAndSplitSentence(sentence,lemmatizer));
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
    Set<String> stopwords = CleanUtilis.getBlackList(getStopWordPath());
    sentence = sentence.toLowerCase();
    sentence = sentence.replaceAll("[.!?\\\\/|<>\'\"+;%$#@&\\^\\(\\),-]\\*", "");
    List<String> textLemmatized = lemmatizer.lemmatize(sentence);
    Iterator<String> it = textLemmatized.iterator();
    while(it.hasNext()){
      String str = it.next();
      if(stopwords.contains(str) || str.length()<=2)
        it.remove();
    }
    return textLemmatized;
  }


  /**
   * Build the matrix used for the SVD method.
   * @param sentences - the sentence's list of the document.
   * @return A MatrixRepresentation object that contains a double[][] matrix and a list of unique words.
   * An matrix A (nm) is a matrix that has n words and m sentences that make up the document. 
   * Each cell in the matrix represents the weight that the term has in the corresponding sentence.
   */
  public  MatrixRepresentation buildMatrixA(List<List<String>> sentences){

    List<String> wordList = new ArrayList<String>();
    


    //crea la lista di word
    for(List<String> sentence : sentences){
      for(String word : sentence){
        if(!wordList.contains(word)){
          wordList.add(word);
        }
      }
    }
    System.out.println("senteces number -> "+sentences.size());
    System.out.println("words number -> "+wordList.size());
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
  private   List<Keyword> getKeywordList(MatrixRepresentation matrixA, RealMatrix U, int threshold){

    double[] bestColumn = U.getColumn(0);

    HashMap<Integer,Double> bestIndex = new HashMap<Integer,Double>();
    List<Keyword> keywordList = new ArrayList<Keyword>();

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
      return null;
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
    if(sentenceJ.size()==0)
      return 0;
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

  /**
   * @return
   */
  public String getMainDirectory() {
    return mainDirectory;
  }

  /**
   * @param mainDirectory
   */
  public void setMainDirectory(String mainDirectory) {
    this.mainDirectory = mainDirectory;
  }

  /**
   * @return the path of the stopwords file
   */
  public  String getStopWordPath() {
    return stopWordPath;
  }

  /**
   * @param stopWordPath
   */
  public  void setStopWordPath(String stopWordPath) {
    LSAKeywordExtractor.stopWordPath = stopWordPath;
  }

  
  public double translateFunction(double x){
    //System.out.println(x);
    
    return (Math.atan(5 * x - 3)/Math.PI)+(0.5);
        
  }

}

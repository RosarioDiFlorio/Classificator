package eu.innovation.engineering.LSA.keywordExtractor;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.util.CleanUtilis;
import eu.innovationengineering.lang.ISO_639_1_LanguageCode;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;
import eu.innovationengineering.word2vec.common.Constants;
import eu.innovationengineering.word2vec.common.request.bean.VectorListRequestBean;
import eu.innovationengineering.word2vec.service.rest.impl.Word2vecServiceImpl;

/**
 * @author Rosario
 * @author Luigi
 *
 */
public class LSACosineKeywordExtraction implements KeywordExtractor {  



  private String mainDirectory = "";
  private String stopWordPath= "data/stopwords/stopwords_en.txt";
  private List<String> toCompare;
  private float[][] toCompareVectors;


  public LSACosineKeywordExtraction(String mainDir,String glossaryName) throws JsonParseException, JsonMappingException, IOException {
    setMainDirectory(mainDir);
    setStopWordPath(getMainDirectory() + stopWordPath);

    ObjectMapper mapper = new ObjectMapper();
    Map<String,List<String>> glossaryMap = mapper.readValue(new File(glossaryName),new TypeReference<Map<String,List<String>>>() {});
    toCompare = new ArrayList<>();
    for(String glossary: glossaryMap.keySet()){
      toCompare.addAll(glossaryMap.get(glossary));
    }  
    List<List<String>> textList = new ArrayList<>();
    for(String toC: toCompare){
      List<String> tmp = new ArrayList<>();
      tmp.add(toC);
      textList.add(tmp);
    }
    toCompareVectors = returnVectorsFromTextList(textList);

  }




  @Override
  public List<List<Keyword>> extractKeywordsFromTexts(List<String> toAnalyze, int numKeywordsToReturn) throws Exception {
    List<List<Keyword>> toReturn = new ArrayList<List<Keyword>>();
    List<List<String>> textList = new ArrayList<>();
    for(String text: toAnalyze){
      List<Keyword> keywordList = new ArrayList<Keyword>();
      List<List<String>> sentenceList = createSentencesFromText(text);
      MatrixRepresentation matrixA = buildMatrixA(sentenceList,toCompareVectors);
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
    //sentence = sentence.replaceAll("[.!?\\\\/|<>\'\"+;%$#@&\\^\\(\\),-]\\*", "");
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
   * @throws IOException 
   */
  public  MatrixRepresentation buildMatrixA(List<List<String>> sentences,float[][] toCompareVectors) throws IOException{
    List<String> wordList = new ArrayList<String>();

    //crea la lista di word
    List<List<String>> textList = new ArrayList<>();
    for(List<String> sentencce : sentences){
      for(String word : sentencce){
        if(!wordList.contains(word)){
          List<String> tmp = new ArrayList<>();
          wordList.add(word); 
          tmp.add(word);
          textList.add(tmp);
        }
      }
    }
    float[][] wordVectors = returnVectorsFromTextList(textList);

    //crea la matrice
    Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(wordList.size(),sentences.size());
    int row=0;
    int column=0;

    for(int i = 0;i<wordList.size();i++){
      column=0;
      String word = wordList.get(i);
      double weigth = 0;
      for(int j = 0;j <toCompareVectors.length;j++){ 
        weigth += cosineSimilarity(wordVectors[i], toCompareVectors[j]);
      }
      //weigth = weigth / toCompareVectors.length-1;
      for(List<String> sentence : sentences){
        if(sentence.contains(word))
          weigth = weigth *  Tf(word, sentences, column);
        else
          weigth = 0;
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
  private static List<Keyword> getKeywordList(MatrixRepresentation matrixA, RealMatrix U, int threshold){

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
  private static  int max(double[] bestColumn) {
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
  public String getStopWordPath() {
    return stopWordPath;
  }

  /**
   * @param stopWordPath
   */
  public void setStopWordPath(String stopWordPath) {
    this.stopWordPath = stopWordPath;
  }

  public float[][] returnVectorsFromTextList(List<List<String>> textList) throws IOException{


    VectorListRequestBean vectorListRequest = new VectorListRequestBean();
    vectorListRequest.setDocs(textList);
    //chiamo il wordToVec per calcolare il vettore delle stinghe ottenute
    WebClient webClient = WebClient.create("http://smartculture-projects.innovationengineering.eu/word2vec-rest-service/", Arrays.asList(new JacksonJaxbJsonProvider()));

    try (Word2vecServiceImpl word2vecService = new Word2vecServiceImpl()) {      
      word2vecService.setWebClient(webClient);
      return word2vecService.getVectorList(Constants.GENERAL_CORPUS, Constants.ENGLISH_LANG, vectorListRequest);
    }
    catch(Exception e){
      System.out.println(e);
      return null;
    }

  }

  public double cosineSimilarity(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
      for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += vectorA[i] * vectorA[i];
        normB += vectorB[i] * vectorB[i];
      }   
    }

    if(dotProduct == 0 || (normA * normB) == 0)
      return 0;
    else
      return ((dotProduct) / (Math.sqrt(normA * normB)));
  }

  public List<String> readGlossay(String pathFile) throws IOException{
    List<String> toReturn = new ArrayList<>();
    String line= "";

    FileReader fstream = new FileReader(pathFile);
    BufferedReader br = new BufferedReader(fstream);

    while ((line = br.readLine()) != null) {
      if(!line.contains(" "))
        toReturn.add(line);
    }
    return toReturn;
  }


  public List<String> getToCompare() {
    return toCompare;
  }


  public void setToCompare(List<String> toCompare) {
    this.toCompare = toCompare;
  }



}

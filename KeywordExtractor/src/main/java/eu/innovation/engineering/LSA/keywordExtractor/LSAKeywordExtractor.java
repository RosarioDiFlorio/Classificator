package eu.innovation.engineering.LSA.keywordExtractor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;

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
  public static MatrixRepresentation buildMatrixA(List<String> chunks){
    
    List<String> wordList = new ArrayList<String>();
    
    for(String chunk : chunks){
      if(!wordList.contains(chunk)){
          wordList.add(chunk);
      }
    }
    
    
    
    
    return null;
  }

  /**
   * @param <E>
   * @return toDefine
   */
  public static Array2DRowRealMatrix SVD(MatrixRepresentation matrixA){  
    return null;
  }
  
  
  private static  List<Keyword> getKeywordList(MatrixRepresentation matrixA, Array2DRowRealMatrix SVDResult){
    return null;
  }
  
  
  private static float Tf(String word,List<String>chunks){
    return 0;
  }
  
  private static float Isf(String word,List<String> chunks){
    return 0;
  }
  
  
}

package eu.innovation.engineering.LSA.keywordExtractor;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;

/**
 * @author Rosario
 * @author Luigi
 */
public class MatrixRepresentation {
  
  /**
   * Array2DRowRealMatrix Object
   */
  private Array2DRowRealMatrix matrixA;

  /**
   * List of unique words of the document
   */
  private List<String> tokenList;

  /**
   * @return Array2DRowRealMatrix object
   */
  public Array2DRowRealMatrix getMatrixA() {
    return matrixA;
  }
  /**
   * @param matrixA Array2DRowRealMatrix
   */
  public void setMatrixA(Array2DRowRealMatrix matrixA) {
    this.matrixA = matrixA;
  }
  /**
   * @return the list of words
   */
  public List<String> getTokenList() {
    return tokenList;
  }
  /**
   * @param tokenList
   */
  public void setTokenList(List<String> tokenList) {
    this.tokenList = tokenList;
  }

}

package eu.innovation.engineering.LSA.keywordExtractor;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;

public class MatrixRepresentation {
  private Array2DRowRealMatrix matrixA;
  
  private List<String> tokenList;
  
  public Array2DRowRealMatrix getMatrixA() {
    return matrixA;
  }
  public void setMatrixA(Array2DRowRealMatrix matrixA) {
    this.matrixA = matrixA;
  }
  public List<String> getTokenList() {
    return tokenList;
  }
  public void setTokenList(List<String> tokenList) {
    this.tokenList = tokenList;
  }

}

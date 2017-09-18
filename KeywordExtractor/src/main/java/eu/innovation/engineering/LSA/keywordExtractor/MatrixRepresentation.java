package eu.innovation.engineering.LSA.keywordExtractor;
import java.util.List;

public class MatrixRepresentation {
  private float[][] matrixA;
  private List<String> tokenList;
  
  public float[][] getMatrixA() {
    return matrixA;
  }
  public void setMatrixA(float[][] matrixA) {
    this.matrixA = matrixA;
  }
  public List<String> getTokenList() {
    return tokenList;
  }
  public void setTokenList(List<String> tokenList) {
    this.tokenList = tokenList;
  }

}

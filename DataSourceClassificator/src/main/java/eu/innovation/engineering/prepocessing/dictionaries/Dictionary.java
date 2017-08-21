package eu.innovation.engineering.prepocessing.dictionaries;

import java.util.HashSet;

public class Dictionary {

  
  private HashSet<String> keywords;
  private float[] vector;
  
  public HashSet<String> getKeywords() {
    return keywords;
  }
  
  public void setKeywords(HashSet<String> keywords) {
    this.keywords = keywords;
  }
  
  public float[] getVector() {
    return vector;
  }
  
  public void setVector(float[] vector) {
    this.vector = vector;
  }
  
  
  
}

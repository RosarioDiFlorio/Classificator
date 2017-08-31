package eu.innovation.engineering.prepocessing.featurextractor;

import java.util.HashMap;

public class Dictionary {

  
  //private HashSet<Keyword> keywords;
  private HashMap<String,Double> keywords;
  private float[] vector;
  private float avg;
  private float variance;
  

  
  public float[] getVector() {
    return vector;
  }
  
  public void setVector(float[] vector) {
    this.vector = vector;
  }

  public float getAvg() {
    return avg;
  }

  public void setAvg(float avg) {
    this.avg = avg;
  }

  public float getVariance() {
    return variance;
  }

  public void setVariance(float variance) {
    this.variance = variance;
  }

  public HashMap<String,Double> getKeywords() {
    return keywords;
  }

  public void setKeywords(HashMap<String,Double> keywords) {
    this.keywords = keywords;
  }
  
  
  
}

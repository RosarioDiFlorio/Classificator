package eu.innovation.engineering.prepocessing.featurextractor;

import java.util.HashSet;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

public class Dictionary {

  
  private HashSet<Keyword> keywords;
  private float[] vector;
  private float avg;
  private float variance;
  
  public HashSet<Keyword> getKeywords() {
    return keywords;
  }
  
  public void setKeywords(HashSet<Keyword> keywords) {
    this.keywords = keywords;
  }
  
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
  
  
  
}

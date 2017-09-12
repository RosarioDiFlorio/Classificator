package eu.innovation.engineering.util.featurextractor;

import java.util.Arrays;

public class SourceVector {
  
  private String id;
  private String category;
  private float[] vector;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getCategory() {
    return category;
  }
  public void setCategory(String category) {
    this.category = category;
  }
  public float[] getVector() {
    return vector;
  }
  public void setVector(float[] vector) {
    this.vector = vector;
  }
  @Override
  public String toString() {
    return "SourceVector [id=" + id + ", category=" + category + ", vector=" + Arrays.toString(vector) + "]";
  }
  
  

}

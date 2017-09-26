package eu.innovation.engineering.util.featurextractor;

import java.util.Arrays;
import java.util.List;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

public class SourceVector {
  
  private String id;
  private String category;
  private String title;
  private List<Keyword> keywords;
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
    return "SourceVector [id=" + id + ", category=" + category + ", title=" + title + ", keywords=" + getKeywords() + ", vector=" + Arrays.toString(vector) + "]";
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public List<Keyword> getKeywords() {
    return keywords;
  }
  public void setKeywords(List<Keyword> keywords) {
    this.keywords = keywords;
  }

  
  

}

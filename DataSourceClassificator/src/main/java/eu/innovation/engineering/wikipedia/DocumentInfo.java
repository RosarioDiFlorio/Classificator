package eu.innovation.engineering.wikipedia;

import java.util.Set;

public class DocumentInfo {
  private String id;
  private String title;
  private String text;
  private Set<String> rootCategories;
  
  
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
  }
  public Set<String> getRootCategories() {
    return rootCategories;
  }
  public void setRootCategories(Set<String> rootCategories) {
    this.rootCategories = rootCategories;
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

}

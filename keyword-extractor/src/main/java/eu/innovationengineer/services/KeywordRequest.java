package eu.innovationengineer.services;

import java.util.List;

public class KeywordRequest {

  private String text;
  private List<String> classList;
  public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
  }
  public List<String> getClassList() {
    return classList;
  }
  public void setClassList(List<String> classList) {
    this.classList = classList;
  }
  
  
}

package eu.innovationengineer.services;

import java.util.List;

import eu.innovationengineer.confidence.ConfidenceResult;
import eu.innovationengineering.extractor.Keyword;

public class KeywordResponse {
  
  private int status; 
  private String message;
  private List<ConfidenceResult> confidences;
  private List<Keyword> keywords;
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }
  public int getStatus() {
    return status;
  }
  public void setStatus(int status) {
    this.status = status;
  }
  public List<ConfidenceResult> getConfidences() {
    return confidences;
  }
  public void setConfidences(List<ConfidenceResult> confidences) {
    this.confidences = confidences;
  }
  public List<Keyword> getKeywords() {
    return keywords;
  }
  public void setKeywords(List<Keyword> keywords) {
    this.keywords = keywords;
  }
  @Override
  public String toString() {
    return "ConfidenceResponse [status=" + status + ", message=" + message + ", confidences=" + confidences + ", keywords=" + keywords + "]";
  }
  
  
  
}

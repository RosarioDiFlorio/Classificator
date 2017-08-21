package eu.innovation.engineering.prepocessing.util;


public class Features {

  
  private String label;
  private double score;
  
  
  public Features(String label, double score) {
    super();
    this.label = label;
    this.score = score;
  }

  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public double getScore() {
    return score;
  }
  
  public void setScore(double score) {
    this.score = score;
  }
  
  
}

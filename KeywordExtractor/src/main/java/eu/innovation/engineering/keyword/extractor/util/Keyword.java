package eu.innovation.engineering.keyword.extractor.util;


public class Keyword {
  
  private String text;
  private double Frequency;
  private double inverseFrequency;
  
  
  public Keyword(){
    
  }
  
  public Keyword(String t,Double gf,Double lf){
    this.text=t;
    this.Frequency = gf;
    this.inverseFrequency = lf;
  }
  
  
  public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
  }
  public double getFrequency() {
    return Frequency;
  }
  public void setFrequency(double globalFrequency) {
    this.Frequency = globalFrequency;
  }
  public double getInverseFrequency() {
    return inverseFrequency;
  }
  public void setInverseFrequency(double localFrequency) {
    this.inverseFrequency = localFrequency;
  }
  
  

}

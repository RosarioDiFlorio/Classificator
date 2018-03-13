package eu.innovationengineering.main;

import org.apache.commons.lang.builder.CompareToBuilder;

public class ConfidenceResult  implements Comparable<ConfidenceResult> {
  private String text;
  private double value;
  
  public ConfidenceResult(String text,double value) {
    this.text = text;
    this.value = value;
  }
  
  public String getText() {
    return text;
  }
  
  public void setText(String text) {
    this.text = text;
  }
  
  public double getValue() {
    return value;
  }
  
  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "ConfidenceResult [text=" + text + ", value=" + value + "]";
  }
  
  @Override
  public int compareTo(ConfidenceResult object) {
    return new CompareToBuilder().append(object.getValue(), this.getValue()).toComparison();
  }

}

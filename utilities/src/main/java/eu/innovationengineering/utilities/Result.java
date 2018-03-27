package eu.innovationengineering.utilities;

import org.apache.commons.lang.builder.CompareToBuilder;

public class Result implements Comparable<Result>  {

  private String label;
  private double value;
  
  public Result(String label,double value){
    this.label = label;
    this.value = value;
  }
  
  public Result(){
    
  }
  
  public String getLabel() {
    return label;
  }
  public void setLabel(String label) {
    this.label = label;
  }
  public double getValue() {
    return value;
  }
  public void setValue(double value) {
    this.value = value;
  }
  
  @Override
  public int compareTo(Result object) {
    return new CompareToBuilder().append(object.getValue(), this.getValue()).toComparison();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Result other = (Result) obj;
    if (label == null) {
      if (other.label != null)
        return false;
    }
    else if (!label.equals(other.label))
      return false;
    return true;
  }
  
  
}

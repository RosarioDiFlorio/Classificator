package eu.innovationengineering.extractor;

import org.apache.commons.lang.builder.CompareToBuilder;

public class Keyword implements Comparable<Keyword> {
  
  private String text;
  private double relevance;
  
    public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
  }

  public double getRelevance() {
    return relevance;
  }

  public void setRelevance(double relevance) {
    this.relevance = relevance;
  }
  @Override
  public String toString() {
    return "Keyword [text=" + text + ", relevance=" + relevance + "]";
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((text == null) ? 0 : text.hashCode());
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
    Keyword other = (Keyword) obj;
    if (text == null) {
      if (other.text != null)
        return false;
    }
    else if (!text.equals(other.text))
      return false;
    return true;
  }
  @Override
  public int compareTo(Keyword object) {
    return new CompareToBuilder().append(object.getRelevance(), this.getRelevance()).toComparison();

  }
  
  

}

package eu.innovation.engineering.wikipedia;

import java.util.HashMap;
import java.util.Map;

public class WikiCategoryMatch {
  
  private String term;
  private Map<String,Double> exacts;
  private Map<String,Double> majors;
  private Map<String,Double> minors;
  
  
  public WikiCategoryMatch(){
    setExacts(new HashMap<String,Double>());
    setMajors(new HashMap<String,Double>());
    setMinors(new HashMap<String,Double>());
  }
  
  public void addExact(String match,double value){
    getExacts().put(match, value);
  }
  public void addMajor(String match,double value){
    getMajors().put(match, value);
    updateMaps();
  }
  public void addMinor(String match,double value){
    getMinors().put(match, value);
    updateMaps();
  }

  public String getTerm() {
    return term;
  }
  public void setTerm(String term) {
    this.term = term;
  }

  @Override
  public String toString() {
    return "WikiCategoryMatch [term=" + term + ", exacts=" + exacts + ", majors=" + majors + ", minors=" + minors + "]";
  }

  public Map<String,Double> getExacts() {
    return exacts;
  }

  public void setExacts(Map<String,Double> exacts) {
    this.exacts = exacts;
  }

  public Map<String,Double> getMajors() {
    return majors;
  }

  public void setMajors(Map<String,Double> majors) {
    this.majors = majors;
  }

  public Map<String,Double> getMinors() {
    return minors;
  }

  public void setMinors(Map<String,Double> minors) {
    this.minors = minors;
  }
  
  private void updateMaps(){
    majors.keySet().removeAll(exacts.keySet());
    minors.keySet().removeAll(majors.keySet());
    minors.keySet().removeAll(exacts.keySet());
  }
  
  
}

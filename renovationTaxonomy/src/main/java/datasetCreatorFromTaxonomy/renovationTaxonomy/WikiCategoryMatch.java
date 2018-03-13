package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.util.HashSet;
import java.util.Set;

public class WikiCategoryMatch {

  private Set<String> exacts;
  private Set<String> majors;
  private Set<String> minors;
  
  public WikiCategoryMatch(){
    setExacts(new HashSet<String>());
    setMajors(new HashSet<String>());
    setMinors(new HashSet<String>());
  }
  
  
  public boolean hasExacts(){
    if(exacts.isEmpty())
      return false;
    else 
      return true;
  }
  
  public boolean hasMinors(){
    if(minors.isEmpty())
      return false;
    else 
      return true;
  }
  
  public boolean hasMajors(){
    if(majors.isEmpty())
      return false;
    else 
      return true;
  }
  
  
  public void addExact(String match){
    getExacts().add(match);
  }
  public void addMajor(String match){
    getMajors().add(match);
  }
  public void addMinor(String match){
    getMinors().add(match);
  }
  
  public Set<String> getExacts() {
    return exacts;
  }
  
  public void setExacts(Set<String> exacts) {
    this.exacts = exacts;
  }

  
  public Set<String> getMajors() {
    return majors;
  }

  
  public void setMajors(Set<String> majors) {
    this.majors = majors;
  }

  
  public Set<String> getMinors() {
    return minors;
  }

  
  public void setMinors(Set<String> minors) {
    this.minors = minors;
  }


  @Override
  public String toString() {
    return "WikiCategoryMatch [exacts=" + exacts + ", majors=" + majors + ", minors=" + minors + "]";
  }
  
  
}

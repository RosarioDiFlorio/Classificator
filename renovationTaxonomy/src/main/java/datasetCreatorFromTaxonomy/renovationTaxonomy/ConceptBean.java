package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.util.ArrayList;
import java.util.List;

public class ConceptBean {
  
  private String id;
  private String name;
  private List<String> broaders;
  private String uri;
  private List<String> narrowers;
  private boolean isTopConcept;
 
  
  
  public ConceptBean(){
    broaders = new ArrayList<String>();
    narrowers = new ArrayList<String>();
  }
  
  

  public void addNarrower(String narrower){
    getNarrowers().add(narrower);
  }

  
  public void addBroaders(String broader){
    getBroaders().add(broader);
  }


  public String getId() {
    return id;
  }



  public void setId(String id) {
    this.id = id;
  }



  public String getName() {
    return name;
  }



  public void setName(String name) {
    this.name = name;
  }



  public List<String> getBroaders() {
    return broaders;
  }



  public void setBroaders(List<String> broaders) {
    this.broaders = broaders;
  }



  @Override
  public String toString() {
    return "ConceptBean [id=" + id + ", name=" + name + ", broaders=" + broaders + ", narrowers=" + getNarrowers() + "]";
  }



  public String getUri() {
    return uri;
  }



  public void setUri(String uri) {
    this.uri = uri;
  }



  public List<String> getNarrowers() {
    return narrowers;
  }



  public void setNarrowers(List<String> narrowers) {
    this.narrowers = narrowers;
  }



  public boolean isTopConcept() {
    return isTopConcept;
  }



  public void setTopConcept(boolean isTopConcept) {
    this.isTopConcept = isTopConcept;
  }
  
  
  
}

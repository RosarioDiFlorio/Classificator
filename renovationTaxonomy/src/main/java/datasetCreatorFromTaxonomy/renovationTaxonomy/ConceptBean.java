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
  private WikiCategoryMatch matches;
 
  
  
  public ConceptBean(){
    broaders = new ArrayList<String>();
    narrowers = new ArrayList<String>();
    matches = new WikiCategoryMatch();
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
    return "ConceptBean [id=" + id + ", name=" + name + ", broaders=" + broaders + ", uri=" + uri + ", narrowers=" + narrowers + ", isTopConcept=" + isTopConcept + ", matches=" + matches + "]";
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



  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    ConceptBean other = (ConceptBean) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    }
    else if (!name.equals(other.name))
      return false;
    return true;
  }



  public WikiCategoryMatch getMatches() {
    return matches;
  }



  public void setMatches(WikiCategoryMatch matches) {
    this.matches = matches;
  }
  
  
  
}

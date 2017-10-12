package eu.innovation.engineering.util.preprocessing;

import java.util.Set;

public class CategoryInfo {
  private Set<String> parentSet;
  private Set<String> childSet;
  
  public Set<String> getParentSet() {
    return parentSet;
  }
  public void setParentSet(Set<String> parentSet) {
    this.parentSet = parentSet;
  }
  public Set<String> getChildSet() {
    return childSet;
  }
  public void setChildSet(Set<String> childSet) {
    this.childSet = childSet;
  }  
}

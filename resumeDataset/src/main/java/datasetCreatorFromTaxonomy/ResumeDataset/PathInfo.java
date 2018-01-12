package datasetCreatorFromTaxonomy.ResumeDataset;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class PathInfo implements Comparable<PathInfo>{

  private String name;
  private int value;
  private PathInfo parent;
  public PathInfo(){

  }

  public PathInfo(String name, int value) {
    super();
    this.name = name;
    this.value = value;
  }


  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "PathInfo [name=" + name + ", value=" + value + "]";
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
    PathInfo other = (PathInfo) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    }
    else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public int compareTo(PathInfo object) {
    return new CompareToBuilder().append(this.getValue() * (-1), object.getValue()* (-1)).toComparison();
  }

  public PathInfo getParent() {
    return parent;
  }

  public void setParent(PathInfo parent) {
    this.parent = parent;
  }
}

package eu.innovation.engineering.graph.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class PathInfo implements Comparable<PathInfo>{

  private String name;
  private double value;
  private double lenPath;
  private double inverseCosine;
  private PathInfo parent;
  public PathInfo(){

  }

  
  public PathInfo(String name, double value,double inverseCosine) {
    super();
    this.name = name;
    this.value = value;
    this.inverseCosine = inverseCosine;
  }
  
  
  public PathInfo(String name, double value) {
    super();
    this.name = name;
    this.value = value;
  }


  public double getValue() {
    return value;
  }

  public void setValue(double value) {
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
    return new CompareToBuilder().append(this.getValue(), object.getValue()).toComparison();
  }

  public PathInfo getParent() {
    return parent;
  }

  public void setParent(PathInfo parent) {
    this.parent = parent;
  }

  public double getLenPath() {
    return lenPath;
  }

  public void setLenPath(double lenPath) {
    this.lenPath = lenPath;
  }

  public double getInverseCosine() {
    return inverseCosine;
  }

  public void setInverseCosine(double inverseCosine) {
    this.inverseCosine = inverseCosine;
  }
}

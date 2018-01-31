package eu.innovation.engineering.graph.utility;


public class Edge {
  
  private String parents;
  private String childs;
  private double distance;
  
  
  public Edge(String parents, String childs, double distance) {
    super();
    this.parents = parents;
    this.childs = childs;
    this.distance = distance;
  }
  
  
  @Override
  public String toString() {
    return "Edge [parents=" + parents + ", childs=" + childs + ", distance=" + distance + "]";
  }


  public String getParents() {
    return parents;
  }
  public void setParents(String parents) {
    this.parents = parents;
  }
  public String getChilds() {
    return childs;
  }
  public void setChilds(String childs) {
    this.childs = childs;
  }
  public double getDistance() {
    return distance;
  }
  public void setDistance(double distance) {
    this.distance = distance;
  }
  
  

}

package eu.innovationengineering.persistence;


public class Edge {

  private String from;
  private String to;
  private double value;

  public Edge(String from, String to, double value) {
    this.from = from;
    this.to = to;
    this.value = value;
  }

  public String getFrom() {
    return from;
  }
  public void setFrom(String from) {
    this.from = from;
  }
  public String getTo() {
    return to;
  }
  public void setChilds(String childs) {
    this.to = childs;
  }
  public double getValue() {
    return value;
  }
  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "Node [parents=" + from + ", childs=" + to + ", distance=" + value + "]";
  }

}

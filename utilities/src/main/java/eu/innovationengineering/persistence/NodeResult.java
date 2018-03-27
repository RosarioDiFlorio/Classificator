package eu.innovationengineering.persistence;

import java.util.List;

import eu.innovationengineering.utilities.Result;

public class NodeResult {

  private String label;
  private List<Result> linkedResults;

  public NodeResult(String sourceVertex, List<Result> linkedVertex) {
    super();
    this.label = sourceVertex;
    this.linkedResults = linkedVertex;
  }

  public String getLabel() {
    return label;
  }
  public void setLabel(String label) {
    this.label = label;
  }
  public List<Result> getLinkedResults() {
    return linkedResults;
  }
  public void setLinkedResults(List<Result> linkedResults) {
    this.linkedResults = linkedResults;
  }

  @Override
  public String toString() {
    return "NodeResult [label=" + label + ", linkedResults=" + linkedResults + "]";
  }

}

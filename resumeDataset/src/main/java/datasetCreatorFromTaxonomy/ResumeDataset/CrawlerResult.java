package datasetCreatorFromTaxonomy.ResumeDataset;

import java.util.HashMap;
import java.util.HashSet;

public class CrawlerResult {
  
  private boolean isCrashed;
  private String latestCategoryProcessed;
  private HashSet<String> markedNode;
  private HashMap<String,AdjacencyListRow> adjacencyList;
  
  
  public CrawlerResult(String category){
    isCrashed = false;
    latestCategoryProcessed = category;
    markedNode = new HashSet<String>();
    setAdjacencyList(new HashMap<String, AdjacencyListRow>());
  }
  
  public boolean isCrashed() {
    return isCrashed;
  }
  
  public void setCrashed(boolean isCrashed) {
    this.isCrashed = isCrashed;
  }
  
  public String getLatestCategoryProcessed() {
    return latestCategoryProcessed;
  }
  
  public void setLatestCategoryProcessed(String latestCategoryProcessed) {
    this.latestCategoryProcessed = latestCategoryProcessed;
  }
  
  public HashSet<String> getMarkedNode() {
    return markedNode;
  }
  
  public void setMarkedNode(HashSet<String> markedNode) {
    this.markedNode = markedNode;
  }
  
  public HashMap<String,AdjacencyListRow> getAdjacencyList() {
    return adjacencyList;
  }

  public void setAdjacencyList(HashMap<String,AdjacencyListRow> adjacencyList) {
    this.adjacencyList = adjacencyList;
  }
  



  
  

}

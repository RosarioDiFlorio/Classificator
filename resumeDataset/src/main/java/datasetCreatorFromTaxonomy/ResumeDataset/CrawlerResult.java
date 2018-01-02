package datasetCreatorFromTaxonomy.ResumeDataset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

public class CrawlerResult {

	private boolean isCrashed;
	private int numCategory = 0;
	private String latestCategoryProcessed;
	private HashSet<String> markedNode;
	private HashMap<String,AdjacencyListRow> adjacencyList;
	private PriorityQueue<String> vertexToVisit;

	public CrawlerResult(){
		
	}

	public CrawlerResult(boolean isCrashed, String latestCategoryProcessed, HashSet<String> markedNode,HashMap<String, AdjacencyListRow> adjacencyList, PriorityQueue<String> vertexToVisit) {
		super();
		this.isCrashed = isCrashed;
		this.latestCategoryProcessed = latestCategoryProcessed;
		this.markedNode = markedNode;
		this.adjacencyList = adjacencyList;
		this.numCategory = markedNode.size();
		this.vertexToVisit = vertexToVisit;
	}
	
	
	
	public int getNumCategory() {
		return numCategory;
	}

	public void setNumCategory(int numCategory) {
		this.numCategory = numCategory;
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

  public PriorityQueue<String> getVertexToVisit() {
    return vertexToVisit;
  }

  public void setVertexToVisit(PriorityQueue<String> vertexToVisit) {
    this.vertexToVisit = vertexToVisit;
  }







}

package datasetCreatorFromTaxonomy.ResumeDataset;

import java.util.HashMap;
import java.util.HashSet;

public class CrawlerResult {

	private boolean isCrashed;
	private String latestCategoryProcessed;
	private HashSet<String> markedNode;
	private HashMap<String,AdjacencyListRow> adjacencyList;






	public CrawlerResult(boolean isCrashed, String latestCategoryProcessed, HashSet<String> markedNode,
			HashMap<String, AdjacencyListRow> adjacencyList) {
		super();
		this.isCrashed = isCrashed;
		this.latestCategoryProcessed = latestCategoryProcessed;
		this.markedNode = markedNode;
		this.adjacencyList = adjacencyList;
	}
	
	public CrawlerResult(){
		
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

package datasetCreatorFromTaxonomy.ResumeDataset;

import java.util.HashSet;

public class AdjacencyListRow {
	
	HashSet<String> linkedVertex = new HashSet<String>();
	boolean taxonomyCategory;
	
	public AdjacencyListRow(){
		
	}
	
	public AdjacencyListRow(HashSet<String> linkedVertex, boolean taxonomyCategory) {
		this.linkedVertex = linkedVertex;
		this.taxonomyCategory = taxonomyCategory;
	}


	public HashSet<String> getLinkedVertex() {
		return linkedVertex;
	}


	public void setLinkedVertex(HashSet<String> linkedVertex) {
		this.linkedVertex = linkedVertex;
	}


	public boolean isTaxonomyCategory() {
		return taxonomyCategory;
	}


	public void setTaxonomyCategory(boolean taxonomyCategory) {
		this.taxonomyCategory = taxonomyCategory;
	}
	


}

package utility;

import java.util.HashSet;

public class AdjacencyListRow {
	
	private HashSet<String> linkedVertex = new HashSet<String>();
	private boolean taxonomyCategory;
	
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

  @Override
  public String toString() {
    return "AdjacencyListRow [linkedVertex=" + linkedVertex + ", taxonomyCategory=" + taxonomyCategory + "]";
  }
	


}

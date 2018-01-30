package eu.innovation.engineering.graph.utility;

import java.util.HashSet;

public class AdjacencyListRowVertex {

	private HashSet<Vertex> linkedVertex = new HashSet<Vertex>();
	private boolean taxonomyCategory;
	
	public AdjacencyListRowVertex(){
		
	}
	
	public AdjacencyListRowVertex(HashSet<Vertex> linkedVertex, boolean taxonomyCategory) {
		this.linkedVertex = linkedVertex;
		this.taxonomyCategory = taxonomyCategory;
	}


	public HashSet<Vertex> getLinkedVertex() {
		return linkedVertex;
	}


	public void setLinkedVertex(HashSet<Vertex> linkedVertex) {
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

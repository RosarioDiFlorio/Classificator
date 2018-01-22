package persistence;

import java.util.List;

import utility.Vertex;

/**
 * this class is used to obtain parents or childs vertex from a single element of wikipedia graph saved into DB.
 * @author Luigi
 *
 */
public class EdgeResult {
	
	private String sourceVertex;
	private List<Vertex> linkedVertex;
	
	public EdgeResult(String sourceVertex, List<Vertex> linkedVertex) {
		super();
		this.sourceVertex = sourceVertex;
		this.linkedVertex = linkedVertex;
	}
	
	public String getRootVertex() {
		return sourceVertex;
	}
	public void setRootVertex(String sourceVertex) {
		this.sourceVertex = sourceVertex;
	}
	public List<Vertex> getLinkedVertex() {
		return linkedVertex;
	}
	public void setLinkedVertex(List<Vertex> linkedVertex) {
		this.linkedVertex = linkedVertex;
	}
	
	
	

	
	
	
	
	

}

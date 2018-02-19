package eu.innovation.engineering.graph.utility;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class Vertex implements Comparable<Vertex>{
	
	private String vertexName;
	private double similarity;
	
	public Vertex() {}
	
	public Vertex(String vertexName, double similarity) {
		super();
		this.vertexName = vertexName;
		this.similarity = similarity;
	}
	
	public String getVertexName() {
		return vertexName;
	}
	
	public void setVertexName(String vertexName) {
		this.vertexName = vertexName;
	}
	
	public double getSimilarity() {
		return similarity;
	}
	
	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}

  @Override
  public int compareTo(Vertex o) {
    return new CompareToBuilder().append(this.getSimilarity(), o.getSimilarity()).toComparison();
  }
	
	

}

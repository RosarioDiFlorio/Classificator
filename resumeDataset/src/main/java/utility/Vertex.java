package utility;

public class Vertex {
	
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
	
	

}

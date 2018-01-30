package eu.innovation.engineering.graph.utility;

import java.util.HashMap;
import java.util.HashSet;

public class Category {
	
	private String name;
	private HashMap<String,Category> childs;
	private boolean marked = false;
	
	
	public Category(String name, HashMap<String,Category> childs){
		this.setName(name);
		this.setChilds(childs);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public HashMap<String, Category> getChilds() {
		return childs;
	}
	public void setChilds(HashMap<String, Category> childs) {
		this.childs = childs;
	}
	
	public Category addChild(Category toAdd){
		return childs.put(toAdd.getName(), toAdd);
	}
	public boolean isMarked() {
		return marked;
	}
	public void setMarked(boolean marked) {
		this.marked = marked;
	}
	
	
	
	

}

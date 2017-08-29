package eu.innovation.engineering.util.preprocessing;

import java.util.ArrayList;
import java.util.List;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Concept;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

/**
 *  Paper class
 * @author lomasto
 *
 */
public class Source {

  public String id;
  private String title;
  private String description;
  
  private List<String> texts;
  private ArrayList<Keyword> keywordList;
  private ArrayList<CategoriesResult> categoryList;
  private ArrayList<Concept> conceptList;
 


  public Source(){}
  
  public Source(String id,String title,List<String> texts){
    setId(id);
    setTitle(title);
    setTexts(texts);
  }

  
  public String getId() {
    return id;
  }

  
  public void setId(String id) {
    this.id = id;
  }

  
  public String getTitle() {
    return title;
  }

  
  public void setTitle(String title) {
    this.title = title;
  }

  
  public ArrayList<Keyword> getKeywordList() {
    return keywordList;
  }

  
  public void setKeywordList(ArrayList<Keyword> keywordList) {
    this.keywordList = keywordList;
  }

 
  public ArrayList<CategoriesResult> getCategoryList() {
    return categoryList;
  }


  
  public void setCategoryList(ArrayList<CategoriesResult> categoryList) {
    this.categoryList = categoryList;
  }


  public ArrayList<Concept> getConceptList() {
    return conceptList;
  }

  
  public void setConceptList(ArrayList<Concept> conceptList) {
    this.conceptList = conceptList;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public List<String> getTexts() {
    return texts;
  }


  public void setTexts(List<String> texts) {
    this.texts = texts;
  }

  @Override
  public String toString() {
    return "Source [id=" + id + ", title=" + title + ", description=" + description + ", texts=" + texts + ", keywordList=" + keywordList + ", categoryList=" + categoryList + ", conceptList=" + conceptList + "]";
  }
  

  
  
  
  
}

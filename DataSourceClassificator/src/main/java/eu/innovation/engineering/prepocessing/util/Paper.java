package eu.innovation.engineering.prepocessing.util;

import java.util.ArrayList;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Concept;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

/**
 *  Paper class
 * @author lomasto
 *
 */
public class Paper {

  private String id;
  private String dc_title;
  private String dc_description;
  private ArrayList<Keyword> keywordList;
  private ArrayList<CategoriesResult> categoryList;
  private ArrayList<Concept> conceptList;
 


  public Paper(){}

  
  public String getId() {
    return id;
  }

  
  public void setId(String id) {
    this.id = id;
  }

  
  public String getTitle() {
    return dc_title;
  }

  
  public void setTitle(String title) {
    this.dc_title = title;
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
    return dc_description;
  }


  public void setDescription(String description) {
    this.dc_description = description;
  }
  
  
  
  
}

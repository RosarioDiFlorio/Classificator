package eu.innovation.engineering.util.preprocessing;

import java.util.ArrayList;
import java.util.List;



public class Paper{
  private String id;
  private String dc_title;
  private String dc_description;
  
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  
  public String getTitle() {
    return dc_title;
  }
  
  public void setTitle(String dc_title) {
    this.dc_title = dc_title;
  }

  public String getDescription() {
    return dc_description;
  }
  public void setDescription(String dc_description) {
    this.dc_description = dc_description;
  }
  
  public Source getSource(){
    List<String> texts = new ArrayList<>();
    texts.add(this.dc_title);
    texts.add(this.dc_description);
    Source s = new Source(id,dc_title,texts);
    return s;
  }

  
}
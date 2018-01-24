package eu.innovation.engineering.dataset.utility;

/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class DocumentInfo {
  private String id;
  private String title;
  private String text;
  
  
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getText() {
    return text;
  }
  public void setText(String text) {
    this.text = text;
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DocumentInfo other = (DocumentInfo) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    }
    else if (!id.equals(other.id))
      return false;
    return true;
  }
  @Override
  public String toString() {
    return "DocumentInfo [id=" + id + ", title=" + title + ", text=" + text + "]";
  }

}

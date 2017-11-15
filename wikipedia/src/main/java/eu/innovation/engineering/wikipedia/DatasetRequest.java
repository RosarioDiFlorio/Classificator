package eu.innovation.engineering.wikipedia;

import java.util.HashSet;
import java.util.Set;

public class DatasetRequest {
  private Set<String> categories;

  public Set<String> getCategories() {
    return categories;
  }

  public void setCategories(Set<String> categories) {
    this.categories = categories;
  }

}

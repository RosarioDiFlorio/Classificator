package eu.innovation.engineering.dataset.utility;

import java.io.File;

/**
 * @author Rosario
 */
public class DatasetRequest {
  private File taxonomyCSV;
  private int limitDocuments;
  private boolean online;
  private String name;



  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public File getTaxonomyCSV() {
    return taxonomyCSV;
  }

  public void setTaxonomyCSV(File taxonomyCSV) {
    this.taxonomyCSV = taxonomyCSV;
  }

  public int getLimitDocuments() {
    return limitDocuments;
  }

  public void setLimitDocuments(int limitDocuments) {
    this.limitDocuments = limitDocuments;
  }

}

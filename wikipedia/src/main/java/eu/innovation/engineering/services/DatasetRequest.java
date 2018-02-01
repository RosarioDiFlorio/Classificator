package eu.innovation.engineering.services;

import java.io.File;

/**
 * @author Rosario
 */
public class DatasetRequest {
  private String taxonomyName;
  private int limitDocuments;
  private boolean db;
  private boolean online;
  private String name;
  
  
  private int minCut = 0;
  private int maxCut = 10000;

  private boolean test;




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

  public int getLimitDocuments() {
    return limitDocuments;
  }

  public void setLimitDocuments(int limitDocuments) {
    this.limitDocuments = limitDocuments;
  }

  public int getMinCut() {
    return minCut;
  }

  public void setMinCut(int minCut) {
    this.minCut = minCut;
  }

  public int getMaxCut() {
    return maxCut;
  }

  public void setMaxCut(int maxCut) {
    this.maxCut = maxCut;
  }

  public boolean isTest() {
    return test;
  }

  public void setTest(boolean test) {
    this.test = test;
  }

  public boolean isDb() {
    return db;
  }

  public void setDb(boolean db) {
    this.db = db;
  }

  public String getTaxonomyName() {
    return taxonomyName;
  }

  public void setTaxonomyName(String taxonomyName) {
    this.taxonomyName = taxonomyName;
  }

}

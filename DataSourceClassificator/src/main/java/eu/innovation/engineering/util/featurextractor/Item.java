package eu.innovation.engineering.util.featurextractor;


public class Item {

  private String id;
  private double[] features;
  private String datasource;

  

  
  
  public String getDatasource() {
    return datasource;
  }

  
  public void setDatasource(String datasource) {
    this.datasource = datasource;
  }

  
  public String getId() {
    return id;
  }
  
  public void setId(String name) {
    this.id = name;
  }
  
  public double[] getFeatures() {
    return features;
  }
  
  public void setFeatures(double[] features) {
    this.features = features;
  }
  
  
  
}

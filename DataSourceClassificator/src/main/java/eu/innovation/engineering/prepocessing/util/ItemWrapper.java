package eu.innovation.engineering.prepocessing.util;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class ItemWrapper implements Clusterable {
  private double[] points;
  private Item item;

  public ItemWrapper(Item item) {
      this.item = item;
      this.points = item.getFeatures();
  }

  public Item getItem() {
      return item;
  }
  
  
  @Override
  public double[] getPoint() {
    return this.points;
  }

}

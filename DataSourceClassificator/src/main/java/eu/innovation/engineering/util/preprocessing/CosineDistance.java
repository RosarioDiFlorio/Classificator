package eu.innovation.engineering.util.preprocessing;

import org.apache.commons.math3.ml.distance.DistanceMeasure;

import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;

public class CosineDistance implements DistanceMeasure{

  private static final long serialVersionUID = -332717221572576545L;

  @Override
  public double compute(double[] a, double[] b) {
    double similarity = FeatureExtractor.cosineSimilarity(a, b);
    return Math.acos(similarity);
  }

}

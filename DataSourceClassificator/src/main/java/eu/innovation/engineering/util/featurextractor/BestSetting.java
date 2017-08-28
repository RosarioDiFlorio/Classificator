package eu.innovation.engineering.util.featurextractor;

import org.neuroph.util.TransferFunctionType;

public class BestSetting {

  private int numHiddenNeurons;
  private TransferFunctionType activationFunction;
  
  public int getNumHiddenNeurons() {
    return numHiddenNeurons;
  }
  
  public void setNumHiddenNeurons(int numHiddenNeurons) {
    this.numHiddenNeurons = numHiddenNeurons;
  }
  
  public TransferFunctionType getActivationFunction() {
    return activationFunction;
  }
  
  public void setActivationFunction(TransferFunctionType activationFunction) {
    this.activationFunction = activationFunction;
  }
}

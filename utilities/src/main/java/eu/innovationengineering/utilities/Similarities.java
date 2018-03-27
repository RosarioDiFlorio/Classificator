package eu.innovationengineering.utilities;


public class Similarities {

  
  public static double cosineSimilarityInverse(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
      for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += vectorA[i] * vectorA[i];
        normB += vectorB[i] * vectorB[i];
      }   
    }

    if(dotProduct == 0 || (normA * normB) == 0){
      return  0.001;

    }
    else{
      Double toReturn = Math.acos((dotProduct) / (Math.sqrt(normA * normB)));

      if (toReturn.isNaN())
        return 0.001;

      return Math.acos((dotProduct) / (Math.sqrt(normA * normB)))+0.001;
    }
  }
  
  
  public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
      for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += vectorA[i] * vectorA[i];
        normB += vectorB[i] * vectorB[i];
      }   
    }

    if(dotProduct == 0 || (normA * normB) == 0){
      return 0;

    }
    else{
      return (dotProduct) / (Math.sqrt(normA * normB));
    }
  }
  
}

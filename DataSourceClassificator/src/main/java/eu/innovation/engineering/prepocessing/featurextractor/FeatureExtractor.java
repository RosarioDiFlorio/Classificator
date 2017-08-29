package eu.innovation.engineering.prepocessing.featurextractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.util.featurextractor.Features;
import eu.innovation.engineering.util.preprocessing.Source;



public class FeatureExtractor {
  
  public static HashMap<String,ArrayList<Features>> createFeaturesNormalizedInputDB(ArrayList<Source> listaPaper, HashMap<String, Dictionary> dictionaries) throws IOException{

    
    
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    
    
    HashMap<String, ArrayList<Features>> featuresPapers = createFeaturesInputDB(listaPaper, dictionaries, clusteringDictionaries);

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for(String idPaper : featuresPapers.keySet()){
      for(Features feature : featuresPapers.get(idPaper)){
        if(feature.getScore()>max)
          max=feature.getScore();
        else if(feature.getScore()<min)
          min=feature.getScore();
      }
    }

    for(String idPaper : featuresPapers.keySet()){
      ArrayList<Double> featuresToPrint = new ArrayList<Double>();
      for(Features feature : featuresPapers.get(idPaper)){
        feature.setScore(normalize(feature.getScore(),min,max));
        featuresToPrint.add(feature.getScore());
      }
    }


    System.out.println("MAX-Value: "+ max + "   MIN-Value: "+ min);
    return featuresPapers;

  }
  
  
  public static HashMap<String, ArrayList<Features>> createFeaturesInputDB(ArrayList<Source> listaPaper, HashMap<String, Dictionary> dictionaries, ClusteringKMeans clusteringDictionaries) throws IOException {
    
    //Costruisco i vettori dei set dei peper, ogni paper ha un vettore che rappresenta l'insieme delle fetures.
    float[][] vectorResults = clusteringDictionaries.returnVectorFromFeatures(listaPaper);
    
    //costruisco la matrice, ad ogni paper è associata una lista di features (una per dizionario). 
    HashMap<String, ArrayList<Features>> toReturn= new HashMap<String, ArrayList<Features>>();
    
    //ciclo su tutti i Paper
    for(int i = 0; i<listaPaper.size(); i++){
      //costruisco un arraylist di features che rappresentale features del paper, una per ogni dizionario
      ArrayList<Features> featuresPaper = new ArrayList<Features>();
      //ciclo su tutti i dizionari
      for(String key: dictionaries.keySet()){
        
        //calcolo la similarità tra il dizionario ed il paper
        double score = cosineSimilarity(vectorResults[i], dictionaries.get(key).getVector());
        Features f = new Features(key, score);
        featuresPaper.add(f);
      }
      //aggiungo le features del paper alla matrice di papers
      toReturn.put(listaPaper.get(i).getId(), featuresPaper);
      
    }
    
    return toReturn;
  }
  
  
  //METODO CHE COSTRUISCE LA MATRICE DI TARGET
  public static HashMap<String,ArrayList<Features>> createTargetsInputDB (ArrayList<Source> listaPaper, HashSet<String> classUsed, HashMap<String, Dictionary> dictionaries) throws IOException{

    
    HashMap<String,ArrayList<Features>> toReturn = new HashMap<String,ArrayList<Features>>();

    int count = 0;
    
    //PER OGNI PAPER COSTRUISCO UNA RIGA, LE COLONNE SONO LE CATEGORIE, LE CELLE CONTENGONO LA RELEVANCE SE APPARTENGONO; 0 ALTRIMENTI
    for(Source p : listaPaper){
      
      
        //CREO L'ARRAYLIST DI TARGET PER IL PAPER CORRENTE (QUESTA E' LA LISTA CHE SICURAMENTE VIENE USATA)
        ArrayList<Features> targets = new ArrayList<Features>();
        
        //CREO L'ARRAYLIST DI TARGET DOUBLE PER IL PAPER CORRENTE PROBABILMENTE PER STAMPARE SU FILE, NON RICORDO
        ArrayList<Double> targetsToPrint = new ArrayList<Double>();
        count = 0;
        
        //PER OGNI CATEGORIA SETTO IL VALORE DELLA CELLA
        for(String category : classUsed){

          ArrayList<CategoriesResult> paperCategories = p.getCategoryList();
          if(paperCategories != null){
            //USO UNA VARIABILE BOOLEANA PER SAPERE SE DEVO METTERE 0 COME SCORE, NEL CASO IN CUI LA CATEGORIA NON C'E'.
            boolean add = false;
            //CICLO SULLE CATEGORIE DEL PAPER CORRENTE
            for(CategoriesResult paperCategory : paperCategories){
              //SE CONTIENE LA CATEGORIA, SALVO LA RELEVANCE NELLA FEATURES CORRENTE ED ESCO. 
              if(paperCategory.getLabel().contains(category)){
                Features cat = new Features(category,paperCategory.getScore());
                targets.add(cat);
                targetsToPrint.add(cat.getScore());
                count++;
                add = true;
                break;
              }
            }
            //SE NON è STATA AGGIUNTA NESSUNA CATEGORIA, METTO COME SCORE 0 (SIGNIFICA CHE IL PAPER NON APPARTTIENE ALLA CATEGORIA CORRENTE)
            if(add == false){
              Features cat = new Features(category,0);
              targets.add(cat);
              targetsToPrint.add(cat.getScore());

            }
          }
        }

        toReturn.put(p.getId(), targets);
        
      }



    return toReturn;
  }
  

  public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
      for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += Math.pow(vectorA[i], 2);
        normB += Math.pow(vectorB[i], 2);
      }   
    }

    if(dotProduct == 0 || ((Math.sqrt(normA) * Math.sqrt(normB))) == 0)
      return 0;
    else
      return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  
  public static double normalize (double value, double min, double max){

    return (value - min)/(max - min);
  }

}

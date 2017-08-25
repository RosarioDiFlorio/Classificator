package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.prepocessing.dictionaries.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.dictionaries.Dictionary;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.prepocessing.util.Features;
import eu.innovation.engineering.prepocessing.util.IdAndTarget;
import eu.innovation.engineering.prepocessing.util.Paper;
import eu.innovation.engineering.prepocessing.util.configurator.PathConfigurator;

public class DictionaryBuilder {

   private ObjectMapper mapper;
   private String trainingAndTestFolder = "data/datasets/TrainingAndTest/";

public DictionaryBuilder() {
 mapper = new ObjectMapper();
}
  
  
  public void buildDictionaries() throws IOException{
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    HashMap<String, Dictionary> dictionaries = clusteringDictionaries.clusterWithDatasourceAsItems();

    FeatureExtractor featureExtractor = new FeatureExtractor();
    HashSet<String> categories = (HashSet<String>) featureExtractor.getCategories();

    System.out.println("CATEGORIE");
    System.out.println(categories.toString());

    // PARTE DI CODICE CON IL BALANCER ED UN UNICO DATASET
    /*DatasetBuilder pb = new DatasetBuilder();
    pb.parseDatasetFromJson("datasetTrainingAndTest/datasetSupervisionato.json");
    ArrayList<Paper> listaPaper = pb.getListPapers();
    PaperBalancer balancer = new PaperBalancer();
    System.out.println("Starting balancer for training and test ");
    Bilancio le classi in modo da avere un trainingSet e un testSet bilanciato
    balancer.balancer(listaPaper,categories);
    System.out.println("End balancer for training and test");    
    ArrayList<Paper> trainingSet = balancer.getTrainingSet();
    ArrayList<Paper> testSet = balancer.getTestSet();*/
    ////////////////////////////////////////////////////

    //PARTE DI CODICE SENZA BALANCER E DUE DATASET DIVERSI
    DatasetBuilder pbTraining = new DatasetBuilder();
    pbTraining.parseDatasetFromJson(PathConfigurator.trainingAndTestFolder+"train.json");
    ArrayList<Paper> trainingSet = pbTraining.getListPapers();

    //STAMPO IL DATASET DI TRAINING CON I PAPER DIVISI PER CLASSI, PER VEDERE COME E' BILANCIATO
    System.out.println("\n DATASET DI TRAINING \n");
    HashMap<String,ArrayList<Paper>>trainingPapersForCategory=categoryListWithAssociatePapers(trainingSet,categories);
    for(String key: trainingPapersForCategory.keySet()){
      System.out.println(key+": "+trainingPapersForCategory.get(key).size());
    }

    //STAMPO SU FILE LE CLASSI CONTENENTI I PAPER DI TRAINING 
    FileWriter fileWriterpaperForCategory = new FileWriter("papersForCategory.txt"); 
    for(String key : trainingPapersForCategory.keySet()){
      fileWriterpaperForCategory.write("\n"+key.toUpperCase()+"\n");
      for(Paper p : trainingPapersForCategory.get(key)){
        fileWriterpaperForCategory.write(p.getId()+"\n");
      }
    }

    fileWriterpaperForCategory.flush();
    fileWriterpaperForCategory.close();

    DatasetBuilder pbTesting= new DatasetBuilder();
    pbTesting.parseDatasetFromJson(PathConfigurator.trainingAndTestFolder+"test.json");
    ArrayList<Paper> testSet = pbTesting.getListPapers();
    ///////////////////////////////////////////////


    // CREO LE MATRICI DI FEATURES E DI TARGET PER I DATASET DI TRAINING E TEST
    HashMap<String, ArrayList<Features>> featuresPapersTraining = featureExtractor.createFeaturesNormalizedInputDB(trainingSet,dictionaries);
    HashMap<String, ArrayList<Features>> targetsPapersTraining = featureExtractor.createTargetsInputDB(trainingSet, categories, dictionaries);
    HashMap<String, ArrayList<Features>> featuresPapersTest = featureExtractor.createFeaturesNormalizedInputDB(testSet,dictionaries);
    HashMap<String, ArrayList<Features>> targetsPapersTest = featureExtractor.createTargetsInputDB(testSet, categories, dictionaries);

    HashMap<IdAndTarget,ArrayList<Features>> featuresPapersTrainingWithTarget = loadTargetForDosuments(featuresPapersTraining,targetsPapersTraining,categories);
    HashMap<IdAndTarget,ArrayList<Features>> featuresPapersTestWithTarget = loadTargetForDosuments(featuresPapersTest,targetsPapersTest,categories);
    
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.dictionariesFolder+"dictionaryForTraining.json"), featuresPapersTrainingWithTarget );
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.dictionariesFolder+"dictionaryForTraining.json"), featuresPapersTestWithTarget );
  }
  
//METODO CHE RESITUISCE UN HASHMAP DI CATEGORIA, PER OGNI CATEGORIA LA LISTA DI PAPER CHE APPARTENGONO
  private HashMap<String, ArrayList<Paper>> categoryListWithAssociatePapers(ArrayList<Paper> trainingSet, HashSet<String> categories) {
    HashMap<String, ArrayList<Paper>> categoryWithPapers = new HashMap<String, ArrayList<Paper>>();

    for(String category : categories){
      ArrayList<Paper> toInsert = new ArrayList<Paper>();
      for(Paper p: trainingSet){
        if(p.getCategoryList()!=null && !p.getCategoryList().isEmpty()){
          if(p.getCategoryList().get(0).getLabel().contains(category.replace("/", ""))){
            toInsert.add(p);
          }
          categoryWithPapers.put(category, toInsert);
        }
      }
    }
    return categoryWithPapers;
  }
  
  //METODO CHE ASSOCIA OGNI DOCUMENTO ALLA CLASSE D?APPARTENENZA, SALVANDO ID E TARGET IN UN OGGETTO DI TIPO IdAndTarget
  private HashMap<IdAndTarget, ArrayList<Features>> loadTargetForDosuments(HashMap<String, ArrayList<Features>> featuresPapersTraining,
      HashMap<String, ArrayList<Features>> targetsPapersTraining, HashSet<String> categories) {
    HashMap<IdAndTarget, ArrayList<Features>> toReturn = new HashMap<>();
    for(String key : featuresPapersTraining.keySet()){
      ArrayList<Features> targetsCurrentPaper = targetsPapersTraining.get(key);
      double currentValue=0;
      String currentTarget="";
      Iterator<String> iterator = categories.iterator();
      for(Features target : targetsCurrentPaper){
        String targetIter = iterator.next();
        if(target.getScore()>currentValue){          
          currentValue=target.getScore();
          currentTarget = targetIter;
        }

      }
      IdAndTarget  idAndTarget = new IdAndTarget(key,currentTarget);
      toReturn.put(idAndTarget, featuresPapersTraining.get(key));
      System.out.println(currentTarget);
    }


    return toReturn;
  }
  
  /**
   * Example Main
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException{
    DictionaryBuilder db = new DictionaryBuilder();
    db.buildDictionaries();
  }

  
  
}

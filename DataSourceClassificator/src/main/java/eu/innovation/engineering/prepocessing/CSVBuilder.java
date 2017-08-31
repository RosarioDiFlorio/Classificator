package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.featurextractor.Features;
import eu.innovation.engineering.util.featurextractor.IdAndTarget;
import eu.innovation.engineering.util.preprocessing.Source;

/**
 * crea i file cvs di train e test. Si deve specificare con la variabile testWithoutLabel del metodo buildCSV se il dataset di test ha labels o no
 * @author lomasto
 *
 */
public class CSVBuilder {

  private static ObjectMapper mapper = new ObjectMapper();

 
  public CSVBuilder() {
    mapper = new ObjectMapper();
  }

  /**
   * Example Main
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException{
    buildCSV();
  }
  

  public static void buildCSV() throws IOException{
    boolean testWithoutLabel = true;
    
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    HashMap<String, Dictionary> dictionaries = clusteringDictionaries.clusterWithDatasourceAsItems(PathConfigurator.dictionariesFolder+"dictionariesSource.json", Configurator.numFeatures);

    FeatureExtractor featureExtractor = new FeatureExtractor();
    HashSet<String> categories = (HashSet<String>) Configurator.getCategories();

    System.out.println("CATEGORIE");
    System.out.println(categories.toString());

    //PARTE DI CODICE SENZA BALANCER E DUE DATASET DIVERSI
    DatasetBuilder pbTraining = new DatasetBuilder();
    pbTraining.parseDatasetFromJson(PathConfigurator.trainingAndTestFolder+"train.json");
    ArrayList<Source> trainingSet = pbTraining.getSourceList();

    //STAMPO IL DATASET DI TRAINING CON I PAPER DIVISI PER CLASSI, PER VEDERE COME E' BILANCIATO
    System.out.println("\n DATASET DI TRAINING \n");
    HashMap<String,ArrayList<Source>>trainingPapersForCategory=categoryListWithAssociatePapers(trainingSet,categories);
    for(String key: trainingPapersForCategory.keySet()){
      System.out.println(key+": "+trainingPapersForCategory.get(key).size());
    }

    //STAMPO SU FILE LE CLASSI CONTENENTI I PAPER DI TRAINING 
    FileWriter fileWriterpaperForCategory = new FileWriter("papersForCategory.txt"); 
    for(String key : trainingPapersForCategory.keySet()){
      fileWriterpaperForCategory.write("\n"+key.toUpperCase()+"\n");
      for(Source p : trainingPapersForCategory.get(key)){
        fileWriterpaperForCategory.write(p.getId()+"\n");
      }
    }

    fileWriterpaperForCategory.flush();
    fileWriterpaperForCategory.close();

    DatasetBuilder pbTesting= new DatasetBuilder();
    pbTesting.parseDatasetFromJson(PathConfigurator.trainingAndTestFolder+"datasetWithoutLabel.json");
    ArrayList<Source> testSet = pbTesting.getSourceList();
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

    createDatasetPython(featuresPapersTrainingWithTarget,categories,"train");
    if(testWithoutLabel)
      createDatasetPythonWithoutCategories(featuresPapersTest,"test"); 
    else
      createDatasetPython(featuresPapersTestWithTarget,categories,"test");
  }

  //METODO CHE RESITUISCE UN HASHMAP DI CATEGORIA, PER OGNI CATEGORIA LA LISTA DI PAPER CHE APPARTENGONO
  private static HashMap<String, ArrayList<Source>> categoryListWithAssociatePapers(ArrayList<Source> trainingSet, HashSet<String> categories) {
    HashMap<String, ArrayList<Source>> categoryWithPapers = new HashMap<String, ArrayList<Source>>();

    for(String category : categories){
      ArrayList<Source> toInsert = new ArrayList<Source>();
      for(Source p: trainingSet){
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
  private static HashMap<IdAndTarget, ArrayList<Features>> loadTargetForDosuments(HashMap<String, ArrayList<Features>> featuresPapersTraining,
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

  private static void createDatasetPythonWithoutCategories(HashMap<String, ArrayList<Features>> featuresPapers, String fileName) throws IOException {
    // TODO Auto-generated method stub
    //Per ogni categoria creo una folder
    String firstLine="id";
    FileWriter writerCSV = new FileWriter(PathConfigurator.pyCSVFolder+fileName+".csv");
    for(int i=0;i<Configurator.numFeatures;i++)
      firstLine+=",F"+i;

    firstLine+="\n";

    writerCSV.write(firstLine);
    String toWrite="";
    for(String id : featuresPapers.keySet()){
      toWrite+=id;
      for(Features f : featuresPapers.get(id)){
        toWrite+=","+f.getScore();
      }
      toWrite+="\n";
      writerCSV.write(toWrite);
      toWrite="";
    }

    writerCSV.flush();
    writerCSV.close();

  }
  /**
   * 
   * @param featuresPapersTrainingWithTarget
   * @param categories
   * @param fileName
   * @throws IOException
   */
  private static void createDatasetPython(HashMap<IdAndTarget, ArrayList<Features>> featuresPapersTrainingWithTarget, HashSet<String> categories, String fileName) throws IOException {
    //Per ogni categoria creo una folder
    String firstLine="labels,id";

    File csvFile = new File(PathConfigurator.pyCSVFolder+fileName+".csv");
    csvFile.createNewFile();
    PrintWriter pWriterCSV = new PrintWriter(csvFile);
    
    for(int i=0;i<Configurator.numFeatures;i++)
      firstLine+=",F"+i;
    
    pWriterCSV.println(firstLine);


    for(String category : categories){
      File dir = new File(PathConfigurator.pyFolder+category);
      dir.mkdir();
      for(IdAndTarget idAndTarget : featuresPapersTrainingWithTarget.keySet()){
        if(idAndTarget.getTarget().equals(category)){
          FileWriter writer = new FileWriter(PathConfigurator.pyFolder+category+"/"+idAndTarget.getId()+".txt");
          String keywordsToWrite=category+","+idAndTarget.getId()+",";
          Iterator iterator = featuresPapersTrainingWithTarget.get(idAndTarget).iterator();

          Features feature = (Features) iterator.next();

          keywordsToWrite+=feature.getScore();

          do{
            //keywordsToWrite+="";
            Features feature2 = (Features) iterator.next();
            keywordsToWrite+=","+feature2.getScore();
          }
          while(iterator.hasNext());


          pWriterCSV.println(keywordsToWrite);
          writer.write(keywordsToWrite);
          writer.flush();
          writer.close();
        }
      }

    }

    pWriterCSV.flush();
    pWriterCSV.close();
  }






}

package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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

  /**
   * Example Main
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException{
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    HashMap<String, Dictionary> dictionaries = clusteringDictionaries.clusterWithDatasourceAsItems(PathConfigurator.dictionariesFolder+"dictionariesSource.json", Configurator.numFeatures);
    buildCSV("train.json",dictionaries,false);
  }


  public static void buildCSV(String path,HashMap<String, Dictionary> dictionaries,boolean withLabel) throws IOException{

 
    FeatureExtractor featureExtractor = new FeatureExtractor();
    HashSet<String> categories = (HashSet<String>) Configurator.getCategories();

    System.out.println("CATEGORIE");
    //System.out.println(categories.toString());

    DatasetBuilder pbTraining = new DatasetBuilder();
    pbTraining.parseDatasetFromJson(path);
    ArrayList<Source> trainingSet = pbTraining.getSourceList();

    HashMap<String,ArrayList<Source>>trainingPapersForCategory=categoryListWithAssociatePapers(trainingSet,categories);

    //STAMPO SU FILE LE CLASSI CONTENENTI I PAPER DI TRAINING 
    FileWriter fileWriterpaperForCategory = new FileWriter("papersForCategory.txt"); 
    for(String key : trainingPapersForCategory.keySet()){
      fileWriterpaperForCategory.write("\n"+key.toUpperCase()+"\n");
      for(Source p : trainingPapersForCategory.get(key)){
        fileWriterpaperForCategory.write(p.getId()+"\n");
      }
    }

    HashMap<String, ArrayList<Features>> featuresPapersTraining = featureExtractor.createFeaturesNormalizedInputDB(trainingSet,dictionaries);
    HashMap<String, ArrayList<Features>> targetsPapersTraining = featureExtractor.createTargetsInputDB(trainingSet, categories, dictionaries);

    HashMap<IdAndTarget,ArrayList<Features>> featuresPapersTrainingWithTarget = loadTargetForDosuments(featuresPapersTraining,targetsPapersTraining,categories);

    path = path.replaceAll("\\.[a-zA-Z]*", "");
    if(withLabel)
      createDatasetPython(featuresPapersTrainingWithTarget,categories,path);
    else     
      createDatasetPythonWithoutCategories(featuresPapersTraining,path);

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
      //System.out.println(currentTarget);
    }


    return toReturn;
  }

  private static void createDatasetPythonWithoutCategories(HashMap<String, ArrayList<Features>> featuresPapers, String fileName) throws IOException {
    // TODO Auto-generated method stub

    String firstLine="id";
    FileWriter writerCSV = new FileWriter(fileName+".csv");
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
    String firstLine="id";

    File csvFile = new File(fileName+".csv");
    csvFile.createNewFile();
    PrintWriter pWriterCSV = new PrintWriter(csvFile);

    for(int i=0;i<Configurator.numFeatures;i++)
      firstLine+=",F"+i;

    for(int i=0;i<Configurator.numLabels;i++)
      firstLine+=",L"+i;


    pWriterCSV.println(firstLine);




    for(String category : categories){
      for(IdAndTarget idAndTarget : featuresPapersTrainingWithTarget.keySet()){
        if(idAndTarget.getTarget().equals(category)){
          String keywordsToWrite= idAndTarget.getId()+",";
          Iterator iterator = featuresPapersTrainingWithTarget.get(idAndTarget).iterator();
          Features feature = (Features) iterator.next();
          keywordsToWrite+=feature.getScore();
          do{
            Features feature2 = (Features) iterator.next();
            keywordsToWrite+=","+feature2.getScore();
          }
          while(iterator.hasNext());

          String currentCategory = idAndTarget.getTarget().replace(" ", "_");
          
          for(int i=0; i<Configurator.Categories.values().length;i++){
            
            if(currentCategory.contains(Configurator.Categories.values()[i].toString())){
              keywordsToWrite+=","+1;
            }
            else
              keywordsToWrite+=","+0;
          }
          
          pWriterCSV.println(keywordsToWrite);
        }
      }

    }

    pWriterCSV.flush();
    pWriterCSV.close();
  }






}

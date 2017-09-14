package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
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



  /**
   * @param path
   * @param dictionaries
   * @param pathCategories
   * @param withLabel
   * @param numLabels
   * @param numFeatures
   * @throws IOException
   */
  public static void buildCSV(String path,HashMap<String, Dictionary> dictionaries,String pathCategories, boolean withLabel, int numLabels, int numFeatures) throws IOException{

 
    FeatureExtractor featureExtractor = new FeatureExtractor();
    List<String> categories = TxtDataReader.getCategories(pathCategories);

    //System.out.println("CATEGORIE");
    //System.out.println(categories.toString());

    DatasetBuilder setTraining = new DatasetBuilder();
    setTraining.parseDatasetFromJson(path);
    ArrayList<Source> trainingSet = setTraining.getSourceList();

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
      createDatasetPython(featuresPapersTrainingWithTarget,categories,path,numFeatures,numLabels);
    else     
      createDatasetPythonWithoutCategories(featuresPapersTraining,path, numFeatures);

  }
  
  //METODO CHE RESITUISCE UN HASHMAP DI CATEGORIA, PER OGNI CATEGORIA LA LISTA DI PAPER CHE APPARTENGONO
  private static HashMap<String, ArrayList<Source>> categoryListWithAssociatePapers(ArrayList<Source> trainingSet, List<String> categories) {
    HashMap<String, ArrayList<Source>> categoryWithPapers = new HashMap<String, ArrayList<Source>>();

    for(String category : categories){
      ArrayList<Source> toInsert = new ArrayList<Source>();
      for(Source p: trainingSet){
        if(p.getCategoryList()!=null && !p.getCategoryList().isEmpty()){
          if(p.getCategoryList().get(0).getLabel().contains(category)){
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
      HashMap<String, ArrayList<Features>> targetsPapersTraining, List<String> categories) {
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

  private static void createDatasetPythonWithoutCategories(HashMap<String, ArrayList<Features>> featuresPapers, String fileName, int numFeatures) throws IOException {
    // TODO Auto-generated method stub

    String firstLine="id";
    FileWriter writerCSV = new FileWriter(fileName+".csv");
    for(int i=0;i<numFeatures;i++)
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
   * @param numLables 
   * @param numFeatures 
   * @throws IOException
   */
  private static void createDatasetPython(HashMap<IdAndTarget, ArrayList<Features>> featuresPapersTrainingWithTarget, List<String> categories, String fileName, int numFeatures, int numLabels) throws IOException {

    String firstLine="id";

    File csvFile = new File(fileName+".csv");
    csvFile.createNewFile();
    PrintWriter pWriterCSV = new PrintWriter(csvFile);

    for(int i=0;i<numFeatures;i++)
      firstLine+=",F"+i;
    for(int i=0;i<numLabels;i++)
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
          for(int i=0; i<categories.size();i++){            
            if(currentCategory.contains(categories.get(i))){
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

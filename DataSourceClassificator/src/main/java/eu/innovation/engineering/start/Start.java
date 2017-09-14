package eu.innovation.engineering.start;

import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.CSVBuilder;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.DictionaryBuilder;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class Start {

  private static final boolean loadDictionariesFromFile = false;
  private static final boolean buildOnlyTestDataset = false;
  public static void main(String[] args) throws IOException{
    String category = "science";
     
    String path = PathConfigurator.rootFolder + category;
    if(!category.equals(""))
      path = PathConfigurator.rootFolder + category +"/";
    
    
    
    int numFeatures = 40;
    int numLabels = TxtDataReader.getCategories(path+"categories.txt").size();
    
    //CREA IL FILE JSON DEI DIZIONARI
    //mainOnlyForDictionaries(path);
    //CREA I FILE JSON DEL DATASET TXT PASSATO( lo lancio sul train, Il test in realtà lo genero con la classe SolrClient)
    mainToGenerateJsonFromTxt(path);
    //CREA I FILE CSV DI TRAIN E TEST
    //mainForCSV(path,numFeatures,numLabels);

  }

  public static void mainForCSV(String path, int numFeatures, int numLabels) throws IOException{
    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    
    HashMap<String, Dictionary> dictionaries = new HashMap<>();
    if(loadDictionariesFromFile)
      dictionaries = dictionaryBuilder.load(path+"dictionaries.json");
    else
      dictionaries = dictionaryBuilder.build(path+"dictionariesSource.json", numFeatures);    

    //Train
    if(!buildOnlyTestDataset)
      CSVBuilder.buildCSV(path+"training.json", dictionaries, path+"categories.txt", true, numLabels, numFeatures);

    //Test
    CSVBuilder.buildCSV(path+"test.json", dictionaries,  path+"categories.txt" , false, numLabels, numFeatures);

  }


  public static void mainOnlyForDictionaries(String path) throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    System.out.println(path);
    dictionaryBuilder.initJsonDataset("dictionariesSource.txt",path);
    String jsonPath = path+"dictionariesSource.json";
    HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, Configurator.numFeatures);      
   

  }
  
  
  public static void mainToGenerateJsonFromTxt(String path) throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    
    //db.buildDataset("training.txt",path);
    db.buildDataset("test.txt",path);
  }

}

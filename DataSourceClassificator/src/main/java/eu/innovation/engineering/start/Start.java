package eu.innovation.engineering.start;
/**
 * @author lomasto
 * Data pre-processing.
 * This class is used for data-preprocessing. With this we can create training and test csv file. There are three phases. 
 * First, create JSON file that contains source for dictionaries. For this, we need txt file were are ID document's (for Solr query).  
 * In second phase, are created training and test json file, starting with file txt (Solr Id).
 * After that, in the final phase, we load or create dictionary with Json file dictionarySource.json and create CSV for training and test    
 */
import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.CSVBuilder;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.DictionaryBuilder;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class Start {


  //**MENU**
  
  //Primo passo, creazione del file dizionaries.json
  private static final boolean buildJsonDictionaries = false;
  
  //Secondo passo creare i file Json di train e test
  private static final boolean buildJsonTraining = false;
  private static final boolean buildJsonTest = true;

  //Terzo passo, decidere se predere i dizionari persistenti o creare altri, creare i csv
  private static final boolean loadDictionariesFromFile = false;
  private static final boolean buildCSVTraining = true;
  private static final boolean buildCSVTest = true;

  //Other
  private static final String category = "science";
  private static final int numFeatures = 45;

  public static void main(String[] args) throws IOException{
    

    String path = PathConfigurator.rootFolder + category;
    if(!category.equals(""))
      path = PathConfigurator.rootFolder + category +"/";

    int numLabels = TxtDataReader.getCategories(path+"categories.txt").size();

    //CREA IL FILE JSON DEI DIZIONARI
    if(buildJsonDictionaries)
      mainOnlyForDictionaries(path);
    //CREA I FILE JSON DEL DATASET TXT PASSATO( lo lancio sul train, Il test in realtà lo genero con la classe SolrClient)
    mainToGenerateJsonFromTxt(path);
    //CREA I FILE CSV DI TRAIN E TEST
    mainForCSV(path,numFeatures,numLabels);

  }

  public static void mainForCSV(String path, int numFeatures, int numLabels) throws IOException{
    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();

    HashMap<String, Dictionary> dictionaries = new HashMap<>();
    if(loadDictionariesFromFile)
      dictionaries = dictionaryBuilder.load(path+"dictionaries.json");
    else
      dictionaries = dictionaryBuilder.build(path+"dictionariesSource.json", numFeatures, path);    

    //Train
    if(buildCSVTraining)
      CSVBuilder.buildCSV(path+"training.json", dictionaries, path+"categories.txt", true, numLabels, numFeatures);

    //Test
    if(buildCSVTest)
      CSVBuilder.buildCSV(path+"test.json", dictionaries,  path+"categories.txt" , false, numLabels, numFeatures);

  }


  public static void mainOnlyForDictionaries(String path) throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    System.out.println(path);
    dictionaryBuilder.initJsonDataset("dictionariesSource.txt",path);
    String jsonPath = path+"dictionariesSource.json";
    HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, numFeatures,path);      


  }


  public static void mainToGenerateJsonFromTxt(String path) throws IOException{
    DatasetBuilder db = new DatasetBuilder();

    if(buildJsonTraining)
      db.buildDataset("training.txt",path);
    if(buildJsonTest)
      db.buildDataset("test.txt",path);
  }

}

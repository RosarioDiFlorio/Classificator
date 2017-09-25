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

import eu.innovation.engineering.LSA.keywordExtractor.LSAKeywordExtractor;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.CSVBuilder;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.DictionaryBuilder;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class Start {


  //**MENU**
  
  //Primo passo, creazione del file dizionaries.json
  private static final boolean buildJsonDictionaries = true;
  
  //Secondo passo creare i file Json di train e test
  private static final boolean buildJsonTraining = true;
  private static final boolean buildJsonTest = false;

  //Terzo passo, decidere se predere i dizionari persistenti o creare altri, creare i csv
  private static final boolean loadDictionariesFromFile = false;
  private static final boolean buildCSVTraining = true;
  private static final boolean buildCSVTest = true;

  //Other
  private static final String category = "";
  private static final int numFeatures = 500;

  public static void main(String[] args) throws IOException{
    
    Start start = new Start();

    String path = PathConfigurator.rootFolder + category;
    if(!category.equals(""))
      path = PathConfigurator.rootFolder + category +"/";

    int numLabels = TxtDataReader.getCategories(path+"categories.txt").size();
    
    KeywordExtractor ke = new LSAKeywordExtractor(PathConfigurator.keywordExtractorsFolder);

    //CREA IL FILE JSON DEI DIZIONARI
    if(buildJsonDictionaries)
      start.createDictionaries(path,ke);
    //CREA I FILE JSON DEL DATASET TXT PASSATO( lo lancio sul train, Il test in realtà lo genero con la classe SolrClient)
    start.generateJsonFromTxt(path,ke);
    //CREA I FILE CSV DI TRAIN E TEST
    start.generateCSV(path,numFeatures,numLabels);

  }

  public  void generateCSV(String path, int numFeatures, int numLabels) throws IOException{
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


  public void createDictionaries(String path, KeywordExtractor ke) throws IOException {

    // CREAZIONE DEL FILE JSON DEI SOURCE DA USARE PER I DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    dictionaryBuilder.initJsonDataset("dictionariesSource.txt",path,ke,"categories.txt");
    String jsonPath = path+"dictionariesSource.json";
    
    // CREAZIONE DEI DIZIONARI CON CLUSTERING
    dictionaryBuilder.build(jsonPath, numFeatures,path);      
    

  }


  public  void generateJsonFromTxt(String path, KeywordExtractor ke) throws IOException{
    DatasetBuilder db = new DatasetBuilder(ke);
    if(buildJsonTraining)
      db.buildDataset("training.txt",path,"categories.txt");
    if(buildJsonTest)
      db.buildDataset("test.txt",path,"categories.txt");
  }

}

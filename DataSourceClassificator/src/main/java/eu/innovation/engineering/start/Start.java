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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.innovation.engineering.LSA.keywordExtractor.LSACosineKeywordExtraction;
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
  private static final boolean buildJsonDictionaries = false;

  //Secondo passo creare i file Json di train e test
  private static final boolean buildJsonTraining = false;
  private static final boolean buildJsonTest = false;

  //Terzo passo, decidere se predere i dizionari persistenti o creare altri, creare i csv
  private static final boolean loadDictionariesFromFile = true;
  private static final boolean buildCSVTraining = true;
  private static final boolean buildCSVTest = true;

  //Other
  private static final String category = "";

  private static final int numFeatures = 500;


  public static void main(String[] args) throws Exception{

    Start start = new Start();

    String path = PathConfigurator.rootFolder + category;
    if(!category.equals(""))
      path = PathConfigurator.rootFolder + category +"/";

    int numLabels = TxtDataReader.getCategories(path+"categories.txt").size();

    KeywordExtractor ke = new LSACosineKeywordExtraction(PathConfigurator.keywordExtractorsFolder,path+"glossaries.json");
    //KeywordExtractor ke = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);

    //CREA IL FILE JSON DEI DIZIONARI
    /*if(buildJsonDictionaries && category.equals("")){
      start.createDictionariesWithAllGlossary(path);
    }
    else*/
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
    List<String> categories = new ArrayList<>();
    String pathToReadcategories = "";
    if(category.equals(""))
      pathToReadcategories = "categories.txt";
    else
      pathToReadcategories ="../categories.txt";
    dictionaryBuilder.initJsonDataset("dictionariesSource.txt",path,ke,pathToReadcategories);
    String jsonPath = path+"dictionariesSource.json";

    // CREAZIONE DEI DIZIONARI CON CLUSTERING
    dictionaryBuilder.build(jsonPath, numFeatures, path);      


  }

  /*public void createDictionariesWithAllGlossary(String path) throws Exception{

    List<String> categories = TxtDataReader.getCategories(path+"categories.txt");
    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();

    KeywordExtractor ke = new LSACosineKeywordExtraction(PathConfigurator.keywordExtractorsFolder,path+"glossaries/travel.json");
    List<Source> listSources = dictionaryBuilder.initJsonDataset("dictionariesSource.txt",path,ke,"categories.txt");

    for(Source source :listSources){
      HashMap<String,ArrayList<Keyword>>  resultsSource = new HashMap<String,ArrayList<Keyword>>();
      ArrayList<Keyword> keywordList = new ArrayList<Keyword>();
      for(String category : categories){
        ke = new LSACosineKeywordExtraction(PathConfigurator.keywordExtractorsFolder,path+"glossaries/"+category+".json");
        List<String> toAnalyze = new ArrayList<String>();
        toAnalyze.add(source.getTitle()+" "+source.getDescription());
        ArrayList<Keyword> results = (ArrayList<Keyword>) ke.extractKeywordsFromTexts(toAnalyze, 4).get(0);
        //keywordList.addAll(results);
        resultsSource.put(category, results);
      }
      System.out.println(source.getTitle()+"\n"+"------------------------------");
      for(String key : resultsSource.keySet()){
        System.out.println("    "+key);
        keywordList = resultsSource.get(key);
        for(Keyword keyword :keywordList){
          System.out.println("      "+keyword.getText()+" "+keyword.getRelevance());
        }
        System.out.println("\n");
      }
      System.out.println("-----------------------------\n\n");
    }

  }*/


  public  void generateJsonFromTxt(String path, KeywordExtractor ke) throws IOException{
    DatasetBuilder dbTraining = new DatasetBuilder(ke);
    DatasetBuilder dbTest = new DatasetBuilder(ke);
    if(buildJsonTraining)
      dbTraining.buildDataset("training.txt",path,"categories.txt",true);
    if(buildJsonTest)
      dbTest.buildDataset("test.txt",path,"categories.txt",false);
  }

}

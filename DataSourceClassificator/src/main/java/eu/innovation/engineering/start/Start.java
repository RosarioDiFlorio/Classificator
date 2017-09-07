package eu.innovation.engineering.start;

import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.CSVBuilder;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.DictionaryBuilder;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class Start {

  private static final boolean readDictionaryFileTXT = true;
  private static final boolean loadDictionariesFromFile = true;
  private static final boolean buildOnlyTestDataset = true;
  public static void main(String[] args) throws IOException{

    //CREA IL FILE JSON DEI DIZIONARI
    //mainOnlyForDictionaries();
    //CREA I FILE JSON DI TRAIN E TEST (Il test in realtà lo genero con la classe SolrClient)
    //mainToGenerateJsonToTrainAndTestTxt();
    //CREA I FILE CSV DI TRAIN E TEST
    mainForCSV();

  }

  public static void mainForCSV() throws IOException{
    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    //String jsonPath=PathConfigurator.dictionariesFolder+"dictionariesSource.json";
    String jsonPathDictionariesAndTrain=PathConfigurator.trainingAndTestFolder+"trainingBig.json";
    HashMap<String, Dictionary> dictionaries = new HashMap<>();
    if(loadDictionariesFromFile)
      dictionaries = dictionaryBuilder.load(PathConfigurator.dictionariesFolder+"dictionaries.json");
    else
      dictionaries = dictionaryBuilder.build(jsonPathDictionariesAndTrain, Configurator.numFeatures);    

    //Train
    if(!buildOnlyTestDataset)
      CSVBuilder.buildCSV(jsonPathDictionariesAndTrain, dictionaries, true);

    //Test
    CSVBuilder.buildCSV(PathConfigurator.trainingAndTestFolder+"dataSourcesWithoutCategory_10000_10000.json", dictionaries, false);


  }


  public static void mainOnlyForDictionaries() throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    String jsonPath=PathConfigurator.dictionariesFolder;
    if(readDictionaryFileTXT) // crea il file json da usare per creare i dizionari con il clustering kmeans
      //dictionaryBuilder.initJsonDataset("dictionariesSource.txt",jsonPath);
      dictionaryBuilder.initJsonDataset("bigDataset.txt",jsonPath);

    //jsonPath = jsonPath+"dictionariesSource.json";
    jsonPath = jsonPath+"bigDataset.json";
    for(int i=10;i<=100;i+=10){
      HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, i);      
    }

  }
  
  
  public static void mainToGenerateJsonToTrainAndTestTxt() throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    
    KeywordExtractor keywordExtractor = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    db.setKeywordExtractor(keywordExtractor);
    db.buildDataset("trainingBig.txt",PathConfigurator.trainingAndTestFolder);    
    //db.buildDataset("dataSourceWithoutCategory.txt", PathConfigurator.trainingAndTestFolder);
  }

}

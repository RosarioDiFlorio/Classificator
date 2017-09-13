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

  private static final boolean loadDictionariesFromFile = false;
  private static final boolean buildOnlyTestDataset = false;
  public static void main(String[] args) throws IOException{

    //CREA IL FILE JSON DEI DIZIONARI
    //mainOnlyForDictionaries();
    //CREA I FILE JSON DEl DATASET TXT PASSATO( lo lancio sul train, Il test in realtà lo genero con la classe SolrClient)
    //mainToGenerateJsonFromTxt();
    //CREA I FILE CSV DI TRAIN E TEST
    mainForCSV();

  }

  public static void mainForCSV() throws IOException{
    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    
    HashMap<String, Dictionary> dictionaries = new HashMap<>();
    if(loadDictionariesFromFile)
      dictionaries = dictionaryBuilder.load(PathConfigurator.dictionariesFolder+"dictionaries.json");
    else
      dictionaries = dictionaryBuilder.build(PathConfigurator.dictionariesFolder+"dictionariesSource.json", Configurator.numFeatures);    

    //Train
    if(!buildOnlyTestDataset)
      CSVBuilder.buildCSV(PathConfigurator.trainingAndTestFolder+"training.json", dictionaries, true);

    //Test
    CSVBuilder.buildCSV(PathConfigurator.trainingAndTestFolder+"test.json", dictionaries, false);


  }


  public static void mainOnlyForDictionaries() throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    String jsonPath=PathConfigurator.dictionariesFolder;
    dictionaryBuilder.initJsonDataset("dictionariesSource.txt",jsonPath);
    jsonPath = jsonPath+"dictionariesSource.json";
    HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, Configurator.numFeatures);      
   

  }
  
  
  public static void mainToGenerateJsonFromTxt() throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    
    KeywordExtractor keywordExtractor = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    db.setKeywordExtractor(keywordExtractor);
    db.buildDataset("training.txt",PathConfigurator.trainingAndTestFolder);    
  }

}

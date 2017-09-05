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

  public static void main(String[] args) throws IOException{

    //CREA IL FILE JSON DEI DIZIONARI
    //mainOnlyForDictionaries();
    //CREA I FILE JSON DI TRAIN E TEST
    mainToGenerateJsonToTrainAndTestTxt();
    //CREA I FILE CSV DI TRAIN E TEST
    mainForCSV();

  }

  public static void mainForCSV() throws IOException{
    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    String jsonPath=PathConfigurator.dictionariesFolder+"dictionariesSource.json";
    HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, Configurator.numFeatures);    

    //Train
    CSVBuilder.buildCSV(PathConfigurator.trainingAndTestFolder+"trainingAndTestTogether.json", dictionaries, true);

    //Test
    CSVBuilder.buildCSV(PathConfigurator.trainingAndTestFolder+"dataSourcesWithoutCategory.json", dictionaries, false);


  }


  public static void mainOnlyForDictionaries() throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    String jsonPath=PathConfigurator.dictionariesFolder;
    if(readDictionaryFileTXT) // crea il file json da usare per creare i dizionari con il clustering kmeans
      dictionaryBuilder.initJsonDataset("dictionariesSource.txt",jsonPath);

    jsonPath = jsonPath+"dictionariesSource.json";
    for(int i=10;i<=100;i+=10){
      HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, i);      
    }

  }
  
  
  public static void mainToGenerateJsonToTrainAndTestTxt() throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    
    KeywordExtractor keywordExtractor = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    db.setKeywordExtractor(keywordExtractor);
    db.buildDataset("trainingAndTestTogether.txt",PathConfigurator.trainingAndTestFolder);    
    //db.buildDataset("dataSourceWithoutCategory.txt", PathConfigurator.trainingAndTestFolder);
  }

}

package eu.innovation.engineering.start;

import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.DictionaryBuilder;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class Start {

  private static final boolean readDictionaryFileTXT = true;


  public static void main(String[] args) throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    String jsonPath=PathConfigurator.dictionariesFolder;
    if(readDictionaryFileTXT) // crea il file json da usare per creare i dizionari con il clustering kmeans
      dictionaryBuilder.buildJson("dictionariesSource.txt",jsonPath);

    jsonPath = jsonPath+"dictionariesSource.json";
    for(int i=10;i<=100;i+=10){
      HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, i);      
    }

  }

}

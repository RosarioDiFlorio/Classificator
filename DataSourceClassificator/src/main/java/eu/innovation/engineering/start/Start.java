package eu.innovation.engineering.start;

import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.DictionaryBuilder;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class Start {

  private static final boolean readDictionaryFileTXT = false;

  public static void main(String[] args) throws IOException {

    // CREAZIONE DEI DIZIONARI

    DictionaryBuilder dictionaryBuilder = new DictionaryBuilder();
    String jsonPath="";
    if(readDictionaryFileTXT)
      jsonPath = dictionaryBuilder.buildJson("dictionariesSource.txt");
    else
      jsonPath = PathConfigurator.dictionariesFolder+"dictionariesSource.json";
    for(int i=10;i<=100;i+=10){
      HashMap<String, Dictionary> dictionaries = dictionaryBuilder.build(jsonPath, i);      
    }
   
  }

}

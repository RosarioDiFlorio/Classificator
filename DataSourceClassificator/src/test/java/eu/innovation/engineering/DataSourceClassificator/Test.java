package eu.innovation.engineering.DataSourceClassificator;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.SourceVectorBuilder;

public class Test {
  
  
  
  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException{

    System.out.println(DatasetBuilder.loadSources(PathConfigurator.applicationFileFolder+"sources.json").size());
    System.out.println(SourceVectorBuilder.loadSourceVectorList(PathConfigurator.applicationFileFolder+"sourceVectors.json").size());
    System.out.println(DatasetBuilder.loadSources(PathConfigurator.applicationFileFolder+"dictionariesCategory/science/trainingScience.json").size());
  }
}

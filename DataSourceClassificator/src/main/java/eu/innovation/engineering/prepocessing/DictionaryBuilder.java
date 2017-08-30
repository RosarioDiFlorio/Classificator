package eu.innovation.engineering.prepocessing;

import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class DictionaryBuilder {

  
  
  
  public static void main(String[] args) throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    String fileName = "train";
    KeywordExtractor ke = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    
    db.setKeywordExtractor(ke);
    db.setFileName(fileName);
    //db.buildDataset();
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    HashMap<String, Dictionary> dictionaries = clusteringDictionaries.clusterWithDatasourceAsItems(fileName);
  }

}
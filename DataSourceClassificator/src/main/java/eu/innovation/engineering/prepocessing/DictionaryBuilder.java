package eu.innovation.engineering.prepocessing;

import java.io.IOException;
import java.util.HashMap;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class DictionaryBuilder {


 public static String buildJson(String fileName) throws IOException{
   DatasetBuilder db = new DatasetBuilder();

   KeywordExtractor ke = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
   db.setKeywordExtractor(ke);

   return db.buildDataset(fileName);
 }

  public static HashMap<String, Dictionary> build(String JsonPath,int kMeans) throws IOException{
   
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    return clusteringDictionaries.clusterWithDatasourceAsItems(JsonPath,kMeans);

  }

}

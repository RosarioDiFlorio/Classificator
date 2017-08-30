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
    db.buildDataset();
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();

    for(int i = 10; i<=180 ;i+=10){
      HashMap<String, Dictionary> dictionaries = clusteringDictionaries.clusterWithDatasourceAsItems(fileName,i);
      int somma=0;
      for(String key : dictionaries.keySet()){
        somma+=dictionaries.get(key).getVariance();
      }
      System.out.println("Varianza con cut: "+i+" vale: "+somma/i);
    }
  }

}

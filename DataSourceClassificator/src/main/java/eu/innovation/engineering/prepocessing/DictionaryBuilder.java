package eu.innovation.engineering.prepocessing;

import java.io.IOException;
import java.util.ArrayList;
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
    return clean(clusteringDictionaries.clusterWithDatasourceAsItems(JsonPath,kMeans),kMeans);

  }
  
  
  
  public static HashMap<String, Dictionary> clean (HashMap<String, Dictionary> dictionariesToClean,int cut){
    
    HashMap<String,Integer> countKeyword = new HashMap<>();
    
    for(String key: dictionariesToClean.keySet()){
      for(String keyKeyword: dictionariesToClean.get(key).getKeywords().keySet()){
        if(countKeyword.containsKey(keyKeyword)){
          countKeyword.put(keyKeyword, countKeyword.get(keyKeyword)+1);
        }else
          countKeyword.put(keyKeyword, 1);
      }
    }
    
    int percentage = (cut*50)/100;
    
    ArrayList<String> toRemove = new ArrayList<>();
    for(String key:countKeyword.keySet()){
      if(countKeyword.get(key)>= percentage){
        toRemove.add(key);
      }
    }
 
    toRemove.stream().forEach(countKeyword::remove);
    
    toRemove.stream().forEach(System.out::println);
    
    return dictionariesToClean;
    
  }
  
  

}

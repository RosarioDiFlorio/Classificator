package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;

public class DictionaryBuilder {




  public  void initJsonDataset(String fileName, String path, KeywordExtractor ke, String categories) throws IOException{
    DatasetBuilder db = new DatasetBuilder(ke);
    db.buildDataset(fileName,path,categories);
  }

  public  HashMap<String, Dictionary> build(String JsonPath,int kMeans,String path) throws IOException{
    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    return clean(clusteringDictionaries.clusterWithDatasourceAsItems(JsonPath,kMeans,path),kMeans);
  }

  public  void save(HashMap<String, Dictionary> dictionaries,String pathToSave) throws JsonGenerationException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathToSave), dictionaries);
    System.out.println("Dictionaries saved into "+pathToSave);
  }

  public  HashMap<String, Dictionary> load(String path) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    HashMap<String, Dictionary> dictionaries = mapper.readValue(new File(path), new TypeReference<HashMap<String,Dictionary>>(){});
    System.out.println("Dictionaries loaded from "+path);
    return dictionaries;
  }

  private  HashMap<String, Dictionary> clean (HashMap<String, Dictionary> dictionariesToClean,int cut){

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
    return dictionariesToClean;
  }



}

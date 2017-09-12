package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.util.featurextractor.SourceVector;
import eu.innovation.engineering.util.preprocessing.Source;

public class SourceVectorBuilder {

  public static void main(String[] args) throws IOException{
    boolean fromSolr = false;
    String fileName = "trainingDatasetMerged.txt";
    String path = PathConfigurator.trainingAndTestFolder;
    String pathWhereSave = PathConfigurator.applicationFileFolder+"sources.json";
    List<Source> sources = new ArrayList<>();
    DatasetBuilder sourceBuilder = new DatasetBuilder();
    if(fromSolr){
      sources = sourceBuilder.buildDataset(fileName, path);
      sourceBuilder.saveSources(sources, pathWhereSave);
    }else{
      sources = sourceBuilder.loadSources(PathConfigurator.applicationFileFolder+"sources.json");
    }
    pathWhereSave = PathConfigurator.applicationFileFolder+"sourceVectors.json";
    saveSourceVectorList(pathWhereSave, createSourceVectorList(sources));
  }
 
  public static List<SourceVector> createSourceVectorList(List<Source> sources) throws IOException{
    List<SourceVector> toReturn = new ArrayList<>();   
    float[][] vectors = ClusteringKMeans.returnVectorsFromSourceList((ArrayList<Source>) sources);    
    for(int i=0;i<sources.size();i++){
      SourceVector sv = new SourceVector();
      sv.setId(sources.get(i).getId());
      sv.setCategory(sources.get(i).getCategoryList().get(0).getLabel());
      sv.setVector(vectors[i]);
      toReturn.add(sv);
    }
    return toReturn;
  }
  
  public static void saveSourceVectorList(String path,List<SourceVector> list) throws JsonGenerationException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), list);
    System.out.println("Source vectors saved into "+path);
  }
  
  public static List<SourceVector> loadSourceVectorList(String path) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    List<SourceVector> loadedList = mapper.readValue(new File(path), new TypeReference<List<SourceVector>>() {});
    System.out.println("Source vectors loaded from "+path);
    return loadedList;
  }
}

package eu.innovation.engineering.prepocessing.featurextractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.config.PathConfigurator;

public class CategoryVector {
  
  
  public static void main(String[] args) throws IOException{
    execute(PathConfigurator.categoriesScience);
    
  }
  
  public static void execute(String path) throws IOException{
    
    ArrayList<List<String>> categoryList = new ArrayList<List<String>>();
    FileReader file = new FileReader(path);
    BufferedReader reader = new BufferedReader(file);
    
    String line = reader.readLine();
    ArrayList<String> category = new ArrayList<String>();
    while(line!=null){
      line = line.replace("/", "");
      category.add(line);
      List<String> categoryToAdd = new ArrayList<String>();
      if(line.contains(" ")){
        String[] split = line.split(" ");
        categoryToAdd.add(split[0]);
        categoryToAdd.add(split[1]);
      }else{
        categoryToAdd.add(line);
      }
      categoryList.add(categoryToAdd);
      line= reader.readLine();
      
    }
    
    float[][] vectors = ClusteringKMeans.returnVectorsFromTextList(categoryList);
    HashMap<String,float[]> categoryAndVectorList = new HashMap<>();
    for(int i=0;i<vectors.length;i++){
      categoryAndVectorList.put(category.get(i), vectors[i]);
    }
    
    String json = new ObjectMapper().writeValueAsString(categoryAndVectorList);
    
    FileWriter writer = new FileWriter(PathConfigurator.categoriesScienceJson);
    
    writer.write(json);
    writer.flush();
    writer.close();
    
    
  }

}

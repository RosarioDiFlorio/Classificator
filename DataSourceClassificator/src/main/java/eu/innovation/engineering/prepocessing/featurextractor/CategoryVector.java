package eu.innovation.engineering.prepocessing.featurextractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.config.PathConfigurator;

public class CategoryVector {


  public static void main(String[] args) throws IOException{
    execute(PathConfigurator.categories+"science.txt",null,"");

  }

  @Autowired
  public static void execute(String pathCategory,String pathDictionaries, String path) throws IOException{

    ArrayList<List<String>> categoryList = new ArrayList<List<String>>();
    FileReader file = new FileReader(pathCategory);
    BufferedReader reader = new BufferedReader(file);

    String line = reader.readLine();
    ArrayList<String> category = new ArrayList<String>();
    while(line!=null){
      category.add(line);
      List<String> categoryToAdd = new ArrayList<String>();
      if(line.contains(" ")){
        String[] split = line.split(" ");
        categoryToAdd.add(split[0]);
        categoryToAdd.add(split[1]);
      }else{
        categoryToAdd.add(line);
      }
      try{
        //leggo i glossari per ogni sottocategoria
        if(pathDictionaries!=null){
          line = line.replace(" ","_");
          FileReader fileDictionary = new FileReader(pathDictionaries+line+"Dictionary.txt");
          BufferedReader readerDictionary = new BufferedReader(fileDictionary);

          String lineDictionary = readerDictionary.readLine();
          while(lineDictionary!=null){
            String[] splitLine= lineDictionary.split(" ");
            for(int i=0; i<splitLine.length;i++){
              categoryToAdd.add(splitLine[i]);
            }
            lineDictionary = readerDictionary.readLine();
          }
        }
      }
      catch(Exception ex){
        System.out.println("Il file"+pathDictionaries+line+"Dictionary.txt"+" non esiste");
      }
    
    categoryList.add(categoryToAdd);
    line= reader.readLine();

  }
  ClusteringKMeans clustering = new ClusteringKMeans();
  float[][] vectors = clustering.returnVectorsFromTextList(categoryList);
  HashMap<String,float[]> categoryAndVectorList = new HashMap<>();
  for(int i=0;i<vectors.length;i++){
    categoryAndVectorList.put(category.get(i), vectors[i]);
  }

  String json = new ObjectMapper().writeValueAsString(categoryAndVectorList);

  FileWriter writer = new FileWriter(path+"vectorCategory.json");

  writer.write(json);
  writer.flush();
  writer.close();


}

}

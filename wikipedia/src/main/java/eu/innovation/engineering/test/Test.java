package eu.innovation.engineering.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.wikipedia.DocumentInfo;
import eu.innovation.engineering.wikipedia.WikipediaMiner;

public class Test {

  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    main3(args);
    //createMapDataset("D:/Development/Datasets/dataset_IBM/");
  }

  private static Map<String, Integer> countLeafs(Map<String, List<String>> mapcsv){
    Map<String, Integer> toReturn = new HashMap<>();
    for(String key:mapcsv.keySet()){
      String rootCat = mapcsv.get(key).get(0);
      if(toReturn.containsKey(rootCat)){
        int count = toReturn.get(rootCat);
        count ++;
        toReturn.replace(rootCat, count);
      }else
        toReturn.put(rootCat, 1);
    }
    return toReturn;
  }

  public static void main3(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    Map<String, List<String>> csvMap = read("wheesbee_cat_recovery.csv", false);
    System.out.println(csvMap.keySet().size());
    int count = 0;

    String pathDataset = "D:/Development/Datasets/dataset_IBM/dataset_tassonomia";
    new File(pathDataset).mkdir();
    Set<String> toExtract = new HashSet<>();
    for(String uriWiki : csvMap.keySet()){

      List<String> parents = csvMap.get(uriWiki);
      String path = pathDataset;
      for(int i =0; i<parents.size();i++){
        path = path+"/"+parents.get(i);
        new File(path).mkdir();
      }
      parents = new ArrayList<>();
      parents.add(path);
      csvMap.replace(uriWiki, parents);
      toExtract.add(uriWiki);
      count++;


      Map<String, Set<DocumentInfo>> results = WikipediaMiner.buildDataset(toExtract, 0, true, 125);
      for(String key : results.keySet()){
        for( DocumentInfo doc: results.get(key)){
          PrintWriter writer = new PrintWriter(new File(csvMap.get(key).get(0)+"/"+doc.getId()));
          writer.println(doc.getText());
          writer.flush();
          writer.close();
        }
        System.out.println("writed in "+csvMap.get(key).get(0));
      }
      count = 0;
      toExtract = new HashSet<>();

    }
  }

  public static void createMapDataset(String path) throws JsonGenerationException, JsonMappingException, IOException{
    Map<String,List<String>> toWrite = new HashMap<>();
    List<String> rootChild = getChilds(path);
    toWrite.put("root", rootChild);

    for(String child:rootChild){
      toWrite.putAll(createMapDatasetTask(path+"/"+child));
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("categories.json"), toWrite);
  }

  public static Map<String,List<String>> createMapDatasetTask(String path) throws JsonGenerationException, JsonMappingException, IOException{
    Map<String,List<String>> toWrite = new HashMap<>();
    List<String> rootChild = getChilds(path);
    if(!rootChild.isEmpty())
      toWrite.put(new File(path).getName(), rootChild);
    for(String child:rootChild){
      List<String> newphewList = getChilds(path+"/"+child);
      if(!newphewList.isEmpty()){
        toWrite.put(child, newphewList);
        for(String newPhew : newphewList){
          toWrite.putAll(createMapDatasetTask(path+"/"+child));
        }
      }
    }
    return toWrite;
  }

  public static List<String> getChilds (String path){
    File dir = new File(path);
    List<String> toReturn = new ArrayList<>();
    if(dir.isDirectory()){
      File[] subDirs = dir.listFiles();
      for(File el: subDirs){
        if(el.isDirectory())
          toReturn.add(el.getName().replace(" ", "_"));
        else
          break;
      }
    }
    Collections.sort(toReturn);
    return toReturn;
  }

  public static Map<String,List<String>> read(String csvFile,boolean labeled) {
    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<String>> dataMap = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      if(labeled)
        line = br.readLine();
      
      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] csvData = line.split(cvsSplitBy); 
        List<String> data = new ArrayList<>();
        if(csvData.length>=2){
          for(int i =0;i<csvData.length-1;i++){
            data.add(csvData[i].trim());
          }
          String key = csvData[csvData.length-1].trim().replace("en.wikipedia.org/wiki/", "");
          if(dataMap.containsKey(key))
            System.out.println(key);
          if(!key.equals(""))
            dataMap.put(key, data);
        }
      }
      return dataMap;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return dataMap;
  }


}

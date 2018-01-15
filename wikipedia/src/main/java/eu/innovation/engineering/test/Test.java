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
    createDatasetFromWikipedia(args);

    //createMapDataset("D:/Development/Datasets/dataset_IBM/");
    //createMapDataset("D:/Development/Datasets/dataset_500xleaf_2012017/");

  }

  private static Map<String, Integer> countLeafs(Map<String, List<List<String>>> mapcsv){
    Map<String, Integer> toReturn = new HashMap<>();
    for(String key:mapcsv.keySet()){
      for(List<String> list: mapcsv.get(key)){
        String rootCat = list.get(0);
        if(toReturn.containsKey(rootCat)){
          int count = toReturn.get(rootCat);
          count ++;
          toReturn.replace(rootCat, count);
        }else
          toReturn.put(rootCat, 1);
      }   
    }
    return toReturn;
  }

  public static void createDatasetFromWikipedia(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    //leggo il file della tassonomia in formato csv
    Map<String, List<List<String>>> csvMap = read("wheesbee_taxonomy.csv", false);
    Map<String, Integer> leafMap = countLeafs(csvMap);

    System.out.println("Total categories -> "+csvMap.keySet().size());


    int rootDocumentLimit = 2000;


    int count = 0;
    //costruisco la struttura delle folder secondo il file csv.
    String pathDataset = "data/dataset_test_1xLeaf_50char";
    new File(pathDataset).mkdir();
    Set<String> toExtract = new HashSet<>();
    for(String uriWiki : csvMap.keySet()){
      List<List<String>> parents = csvMap.get(uriWiki);
      List<String> pathToAdd = new ArrayList<>();
      for(int i =0; i<parents.size();i++){
        String path = pathDataset;
        for(int j=0;j<parents.get(i).size();j++){
          path = path+"/"+parents.get(i).get(j);
          new File(path).mkdir();
        } 
        pathToAdd.add(path);
      }
      parents = new ArrayList<>();
      parents.add(pathToAdd);

      csvMap.replace(uriWiki, parents);
      toExtract.add(uriWiki);
      count++;

      if(count%8 == 0 || count == csvMap.size()){
        Map<String, Set<DocumentInfo>> results = WikipediaMiner.buildDataset(toExtract, 0, true, 1,50);
        System.out.println("Categories done -> "+ count);
        for(String key : results.keySet()){
          System.out.print(key+"-> "+results.get(key).size()+", ");
          for( DocumentInfo doc: results.get(key)){
            for(List<String> list: csvMap.get(key)){
              PrintWriter writer = new PrintWriter(new File(list.get(0)+"/"+doc.getId()));
              writer.println(doc.getText());
              writer.flush();
              writer.close();
            }
          }
          System.out.println("writed in "+csvMap.get(key).get(0));
        }
        toExtract = new HashSet<>();
      }
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

  public static Map<String,List<List<String>>> read(String csvFile,boolean labeled) {
    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<List<String>>> dataMap = new HashMap<>();
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

          if(!key.equals("")){
            if(dataMap.containsKey(key)){
              List<List<String>> toReplace = dataMap.get(key);
              toReplace.add(data);
              dataMap.replace(key, toReplace);
              System.out.println(key+"-->"+toReplace);
            }else{
              List<List<String>> datas = new ArrayList<>();
              datas.add(data);
              dataMap.put(key, datas);
            }
          }


        }
      }
      return dataMap;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return dataMap;
  }


}

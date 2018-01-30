package eu.innovation.engineering.dataset.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatasetUtilities {

  
  public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException{
    Map<String, List<String>> toWrite = createMapDatasetTask("D:/Development/Datasets/dataset_500xleaf_2012017");
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("oldCategories.json"), toWrite);
  }
  
  public static Map<String, List<List<String>>> createStructureFolder(Map<String, List<List<String>>> csvMap,String pathDataset){
    Map<String, List<List<String>>> pathMap = new HashMap<String, List<List<String>>>();
    new File(pathDataset).mkdir();
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
      pathMap.put(uriWiki, parents);
    } 
    return pathMap;   
  }
  
  /**
   * @param pathDataset
   * @param datasetMap
   * @param alreadyWritten
   * @throws FileNotFoundException
   */
  public static void writeDocumentMap(Map<String, List<List<String>>> pathMap,Map<String,Set<DocumentInfo>> documentsMap) throws FileNotFoundException{   
    for(String key : documentsMap.keySet()){
      for( DocumentInfo doc: documentsMap.get(key)){
        int count = 0;
        for(List<String> list: pathMap.get(key)){
          PrintWriter writer = new PrintWriter(new File(list.get(count)+"/"+doc.getId()));
          writer.println(doc.getText());
          writer.flush();
          writer.close();
          count++;
        }
      }
      /*
       * DEBUG PRINTS
       */
      for(List<String> list: pathMap.get(key)){
        System.out.println(key+" saved into "+list.get(0)+", number of documents ->"+documentsMap.get(key).size());
      }
    }
  }
  
  
  public static Map<String,List<List<String>>> readCSV(File csvFile,boolean labeled) {
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

  public static Map<String, Integer> countLeafs(Map<String, List<List<String>>> mapcsv){
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

  /**
   * Return the possible paths.
   * for example a path A/B/C and A/C/D
   * return a list with the path A/B, B/C A/C, C/D
   * @param basePathSrc
   * @return
   */
  public static Set<String> getAllPaths(String basePathSrc){
    Set<String> pathSet = new HashSet<String>();
     Map<String, List<String>> paths = DatasetUtilities.createMapForClassification(basePathSrc);
    for(String path : paths.keySet()){    
      for(String child : paths.get(path)){
        StringBuilder toAdd = new StringBuilder(path);
        toAdd.append("/"+child);
        pathSet.add(toAdd.toString());
      }
    }
    return pathSet;
  }
  
  
  public static Map<String,List<String>> createMapForClassification(String path){
    Map<String,List<String>> classificationMap = new HashMap<>();
    List<String> rootChild = getChildDirectories(path);
    classificationMap.put("root", rootChild);
    for(String child:rootChild){
      classificationMap.putAll(createMapDatasetTask(path+"/"+child));
    }
    return classificationMap;
  }

  private static Map<String,List<String>> createMapDatasetTask(String path){
    Map<String,List<String>> toWrite = new HashMap<>();
    List<String> rootChild = getChildDirectories(path);
    if(!rootChild.isEmpty())
      toWrite.put(new File(path).getName(), rootChild);
    for(String child:rootChild){
      List<String> newphewList = getChildDirectories(path+"/"+child);
      if(!newphewList.isEmpty()){
        toWrite.put(child, newphewList);
        for(String newPhew : newphewList){
          toWrite.putAll(createMapDatasetTask(path+"/"+child));
        }
      }
    }
    return toWrite;
  }

  private static List<String> getChildDirectories (String path){
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
  
}

package eu.innovation.engineering.dataset.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetUtilities {

  private static final Logger logger = LoggerFactory.getLogger(DatasetUtilities.class);


  private String taxonomiesFolder;
  
  public DatasetUtilities(String taxonomiesFolder,String dataFolder){
    this.taxonomiesFolder = taxonomiesFolder;
  }
  
  
  
  /**
   * Method used to save labels on csv file 
   * @param idDocument
   * @param labels
   * @param writer
   * @param splitted
   * @throws IOException
   */
  public static void saveLabelsOnCSV(String idDocument,List<String> labels,FileWriter writer, String[] splitted) throws IOException{

    try{
      if(labels.size()>=3){
        writer.write(splitted[splitted.length-1]+","+idDocument+","+labels.get(0)+","+labels.get(1)+","+labels.get(2)+"\n");
      }
      else
        if(labels.size()>=2){
          writer.write(splitted[splitted.length-1]+","+idDocument+","+labels.get(0)+","+labels.get(1)+"\n");
        }
        else
          if(labels.size()>0){
            writer.write(splitted[splitted.length-1]+","+idDocument+","+labels.get(0)+"\n");
          }
    }
    catch(Exception e){
      System.out.println(splitted[splitted.length-1]);
    }
    writer.flush();

  }


  public Map<String, List<List<String>>> createStructureFolder(Map<String, List<List<String>>> csvMap,String pathDataset){
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
  public void writeDocumentMap(Map<String, List<List<String>>> pathMap,Map<String,Set<DocumentInfo>> documentsMap) throws FileNotFoundException{   
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


  public Set<String> returnCategoriesFromTaxonomyCSV(String filename){
    Set<String> toReturn = new HashSet<>(readTaxomyCSV(filename, false).keySet());
    toReturn = toReturn.stream().map(el->el=el.replace("Category:", "")).collect(Collectors.toSet());
    return toReturn;
  }


  public Map<String,List<List<String>>> readTaxomyCSV(String csvName,boolean labeled) {
    String line = "";
    String cvsSplitBy = ",";
    File csvFile = new File(this.taxonomiesFolder+"/"+csvName+".csv");
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

  /**
   * List all files from a directory and its subdirectories
   * @param directoryName to be listed
   * @return 
   */
  public List<String> listAllFiles(String directoryName, List<String> fileList){
//    directoryName = directoryName.replace("/", "\\");
//    System.out.println(directoryName);
    File directory = new File(directoryName);
    //get all the files from a directory
    File[] fList = directory.listFiles();
    for (File file : fList){
      if (file.isFile()){
        //System.out.println(file.getAbsolutePath());
        fileList.add(file.getAbsolutePath());
      } else if (file.isDirectory()){
        listAllFiles(file.getAbsolutePath(),fileList);
      }
    }
    return fileList;
  }
  /**
   * Return the possible paths.
   * for example a path A/B/C and A/C/D
   * return a list with the path A/B, B/C A/C, C/D
   * @param basePathSrc
   * @return
   */
  public Set<String> listAllPaths(String basePathSrc){
    Set<String> pathSet = new HashSet<String>();
    Map<String, List<String>> paths = createMapForClassification(basePathSrc);
    for(String path : paths.keySet()){    
      for(String child : paths.get(path)){
        StringBuilder toAdd = new StringBuilder(path);
        toAdd.append("/"+child);
        pathSet.add(toAdd.toString());
      }
    }
    return pathSet;
  }



  public Map<String,List<String>> createMapForClassification(String path){
    Map<String,List<String>> classificationMap = new HashMap<>();
    List<String> rootChild = getChildDirectories(path);
    classificationMap.put("root", rootChild);
    for(String child:rootChild){
      classificationMap.putAll(createMapDatasetTask(path+"/"+child));
    }
    return classificationMap;
  }

  private Map<String,List<String>> createMapDatasetTask(String path){
    Map<String,List<String>> toReturn = new HashMap<>();
    List<String> rootChild = getChildDirectories(path);
    if(!rootChild.isEmpty())
      toReturn.put(new File(path).getName(), rootChild);
    for(String child:rootChild){
      List<String> newphewList = getChildDirectories(path+"/"+child);
      if(!newphewList.isEmpty()){
        toReturn.put(child, newphewList);
        for(String newPhew : newphewList){
          toReturn.putAll(createMapDatasetTask(path+"/"+child));
        }
      }
    }
    return toReturn;
  }

  private List<String> getChildDirectories (String path){
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

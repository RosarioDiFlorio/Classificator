package eu.innovation.engineering.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.innovation.engineering.wikipedia.DatasetTask;
import eu.innovation.engineering.wikipedia.DatasetUtilities;
import eu.innovation.engineering.wikipedia.DocumentInfo;

public class DatasetBuilder {

  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    createDatasetFromWikipedia(args);

    //createMapDataset("D:/Development/Datasets/dataset_IBM/");
    //createMapDataset("D:/Development/Datasets/dataset_500xleaf_2012017/");

  }

  

  
  
  
  public static void createDatasetFromWikipedia(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    //leggo il file della tassonomia in formato csv
    String taxonomyCsvFile ="wheesbee_taxonomy.csv";
    String pathDataset = "data/dataset_test_1xLeaf_50char";
    String pathClassificationMap = "classificationMap.json";
    int limitDocForEachLeaf = 1; 
    /*
     * Leggo la tassonomia in formato csv.
     */
    Map<String, List<List<String>>> csvMap = DatasetUtilities.readCSV(taxonomyCsvFile, false);
    /*
     * Costruisco la struttura delle folder utilizzando la mappa creata leggendo il csv.
     */
    Map<String, List<List<String>>> pathMap = DatasetUtilities.createStructureFolder(csvMap, pathDataset);
    /*
     * Creo una mappa di classificazione basata sulla struttura delle folder appena create.
     * Che deve essere utilizzata dal classificatore python per poter riassociare alle label i nomi 
     * delle classi.
     */
    DatasetUtilities.createMapForClassification(pathDataset,pathClassificationMap);
    System.out.println("Total categories -> "+csvMap.keySet().size());
    int count = 0;
    Set<String> toExtract = new HashSet<>();
    for(String uriWiki : pathMap.keySet()){
      toExtract.add(uriWiki);
      count++;
      if(count%8 == 0 || count == pathMap.size()){
        Map<String, Set<DocumentInfo>> results = buildDatasetOnline(toExtract, 0, true, limitDocForEachLeaf);
        System.out.println("Categories done -> "+ count);
        DatasetUtilities.writeDocumentMap(pathMap, results);
        toExtract.clear();
      }
    }
  }

  /**
   * @param pathDataset
   * @param categories
   * @param blackList
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static Map<String,Set<DocumentInfo>> buildDatasetOnline(Set<String> categories,int maxLevel,boolean recursive,int limitDocs) throws IOException, InterruptedException, ExecutionException{
    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<>();
    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, maxLevel,recursive,limitDocs);
      datasetTasks.add(task);
    }
    List<Future<Map<String, Set<DocumentInfo>>>> result = pool.invokeAll(datasetTasks);
    Map<String,Set<DocumentInfo>> datasetMap = new HashMap<>();
    for(Future<Map<String, Set<DocumentInfo>>> future : result){
      datasetMap.putAll(future.get());
    }
    return datasetMap;

  }
  
}

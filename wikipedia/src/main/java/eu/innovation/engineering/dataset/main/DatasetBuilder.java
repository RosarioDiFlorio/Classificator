package eu.innovation.engineering.dataset.main;

import java.io.File;
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

import eu.innovation.engineering.dataset.utility.DatasetRequest;
import eu.innovation.engineering.dataset.utility.DatasetResponse;
import eu.innovation.engineering.dataset.utility.DatasetTask;
import eu.innovation.engineering.dataset.utility.DatasetUtilities;
import eu.innovation.engineering.dataset.utility.DocumentInfo;
import eu.innovation.engineering.dataset.utility.WikiRequest;
import persistence.SQLiteWikipediaGraph;

public class DatasetBuilder implements WikiRequest {

  /**
   * EXAMPLE AND OFFLINE MAIN
   * @param args
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    /*
     * Initialize the Request's Object with the information.
     */
    DatasetRequest request = new DatasetRequest();
    request.setLimitDocuments(1);
    request.setName("test");
    request.setOnline(true);
    request.setTaxonomyCSV(new File("wheesbee_taxonomy.csv"));
    /*
     * Call the method to build the dataset.
     */
    DatasetBuilder builder = new DatasetBuilder();
    builder.buildDataset(request);
  }

  /**
   * @param categories
   * @param maxLevel
   * @param recursive
   * @param limitDocs
   * @return
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static Map<String,Set<DocumentInfo>> onlineDatasetTask(Set<String> categories,int maxLevel,boolean recursive,int limitDocs) throws IOException, InterruptedException, ExecutionException{
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


  /**
   * @param categories
   * @param graph
   * @param limitDocs
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static Map<String,Set<DocumentInfo>> databaseDatasetTask(Set<String> categories,int limitDocs) throws InterruptedException, ExecutionException{
    SQLiteWikipediaGraph graph = new SQLiteWikipediaGraph("databaseWikipediaGraph.db");
    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<>();
    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, graph, limitDocs);
      datasetTasks.add(task);
    }
    List<Future<Map<String, Set<DocumentInfo>>>> result = pool.invokeAll(datasetTasks);
    Map<String,Set<DocumentInfo>> datasetMap = new HashMap<>();
    for(Future<Map<String, Set<DocumentInfo>>> future : result){
      datasetMap.putAll(future.get());
    }
    return datasetMap;
  }

  @Override
  public DatasetResponse buildDataset(DatasetRequest request) {
    /**
     * INIT DATASET.
     **/    
    String basePath = "data/"+request.getName()+"/";
    new File(basePath).mkdirs();
    String pathDataset = basePath+"dataset";
    String classificationMap = basePath+"categories.json";
    try {
      /*
       * Leggo la tassonomia in formato csv.
       */
      Map<String, List<List<String>>> csvMap = DatasetUtilities.readCSV(request.getTaxonomyCSV(), false);
      /*
       * Costruisco la struttura delle folder utilizzando la mappa creata leggendo il csv.
       * ritornando una mappa contente i path per ogni categoria wikipedia.
       */
      Map<String, List<List<String>>> pathMap = DatasetUtilities.createStructureFolder(csvMap, pathDataset);
      /*
       * Creo una mappa di classificazione basata sulla struttura delle folder appena create.
       * Che deve essere utilizzata dal classificatore python per poter riassociare alle label i nomi 
       * delle classi.
       */
      DatasetUtilities.createMapForClassification(pathDataset,classificationMap);
      System.out.println("Categories -> "+pathMap.size());
      /**
       * CORE PHASE.
       */
      int count = 0;
      Set<String> toExtract = new HashSet<>();
      for(String uriWiki : pathMap.keySet()){
        toExtract.add(uriWiki);
        count++;       
        if(count%8 == 0 || count == pathMap.size()){
          System.out.println(((count*100)/pathMap.size())+"%");
          Map<String, Set<DocumentInfo>> results = new HashMap<String, Set<DocumentInfo>>();
          /*
           * Gather all the information completly Online
           */
          if(request.isOnline())
            results = onlineDatasetTask(toExtract, 0, true, request.getLimitDocuments());
          /*
           * Gather the categories from database and the documents Online.
           */
          else
            results = databaseDatasetTask(toExtract, request.getLimitDocuments());                   
          DatasetUtilities.writeDocumentMap(pathMap, results);
          toExtract.clear();
        }
      }
    }
    catch (IOException | InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }









}

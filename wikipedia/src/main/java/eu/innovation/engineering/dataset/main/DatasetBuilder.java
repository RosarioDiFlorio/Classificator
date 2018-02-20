package eu.innovation.engineering.dataset.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.dataset.utility.DatasetTask;
import eu.innovation.engineering.dataset.utility.DatasetUtilities;
import eu.innovation.engineering.dataset.utility.DocumentInfo;
import eu.innovation.engineering.graph.main.AnalyzerGraphWikipedia;
import eu.innovation.engineering.persistence.EdgeResult;
import eu.innovation.engineering.persistence.SQLiteWikipediaGraph;
import eu.innovation.engineering.services.DatasetRequest;
import eu.innovation.engineering.services.DatasetResponse;
import eu.innovation.engineering.services.WikiDataRequest;


public class DatasetBuilder extends SpringMainLauncher implements WikiDataRequest {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(DatasetBuilder.class);
  private boolean running;

  @Autowired
  private SQLiteWikipediaGraph dbGraph;

  @Autowired
  private DatasetUtilities datasetUtilities;

  @Autowired
  AnalyzerGraphWikipedia analyzerWikipedia;

  private String volumeFolder;


  public DatasetBuilder(String volumeFolder){
    this.volumeFolder = volumeFolder;
  }


  /**
   * EXAMPLE AND OFFLINE MAIN
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    mainWithSpring(
        context -> {
          /*
           * Initialize the Request's Object with the information.
           */
          DatasetRequest request = new DatasetRequest();
          request.setLimitDocuments(10);
          request.setTaxonomyName("wheesbee");
          request.setOnline(false);
          request.setTest(true);
          request.setDb(true);
          /*
           * Call the method to build the dataset.
           */
          DatasetBuilder builder = context.getBean(DatasetBuilder.class);
          builder.buildDataset(request);
        },
        args,
        "classpath:properties-config.xml", "classpath:db-config.xml", "classpath:service-config.xml");
  }

  @Override
  public DatasetResponse buildDataset(DatasetRequest request) {
    DatasetResponse response = new DatasetResponse();
    boolean isRunning;
    synchronized (this) {
      isRunning = running;
    }

    if (!isRunning) {
      synchronized (this) {
        running = true;
      }
      /*
       * la taglia del test è uguale al 10% della richiesta massima di documenti.
       */
      int testsize = (request.getLimitDocuments()*10)/100;

      String basePath = volumeFolder+"/";
      new File(basePath).mkdirs();
      basePath = basePath.replace("\\", "/");
      String pathDataset = basePath+"dataset";
      String basePathSrc = pathDataset;

      if(!new File(basePathSrc).exists())
        request.setOnline(true);
      /*
       * Creazione del dataset origin da wikipedia.
       */
      if(request.isOnline()){
        try {
          basePathSrc =  wikipediaDataset(basePath,pathDataset,request);
        }
        catch (Exception e) {
          response.setStatus(500);
          response.setMessage("Error in creation wikipedia dataset\n"+e.getMessage());
        }
      }
      
      int numSourceToCopy = request.getLimitDocuments();    
      List<String> fileList = datasetUtilities.listAllFiles(basePathSrc, new ArrayList<String>());
      Set<String> pathSet = datasetUtilities.listAllPaths(basePathSrc);
      List<String> added = new ArrayList<String>();      
      /*
       * Creazione del dataset di training.
       */
      try {
        String basePathDstTraining = basePath+"datasets_training/";
        new File(basePathDstTraining).mkdir();
        if(request.isTest())
          numSourceToCopy = numSourceToCopy -testsize;
        added = formatDataset(pathSet, basePathDstTraining, basePathSrc, fileList,numSourceToCopy, new ArrayList<String>(), "training",0,0);
        System.out.println("Training ended");
      }catch (Exception e) {
        response.setStatus(500);
        response.setMessage("Error in creation of training dataset\n"+e.getMessage());
      }finally{
        synchronized (this) {
          running = false;
        }
      }
      /*
       * Creazione del dataset di test.
       */
      try{
        if(request.isTest()){
          String basePathDstTest = basePath+"datasets_test/files/";
          new File(basePathDstTest).mkdir();
          formatDataset(pathSet, basePathDstTest, basePathSrc, fileList,testsize,added,"test",request.getMinCut(),request.getMaxCut());
          System.out.println("Test ended");
        } 
        response.setStatus(200);
        response.setMessage("Operation completed");
      }catch (IOException e) {
        response.setStatus(500);
        response.setMessage("Error in creation of test dataset\n"+e.getMessage());
      }finally{
        synchronized (this) {
          running = false;
        }
      }
    }else{
      response.setStatus(400);
      response.setMessage("The process to create the dataset is already running");
    }
    return response;
  }

  public String wikipediaDataset(String basePath, String pathDataset, DatasetRequest request) throws Exception{
    /**
     * INIT DATASET.
     **/    
    String classificationMapPath = basePath+"categories.json";

    /*
     * Leggo la tassonomia in formato csv.
     */
    Map<String, List<List<String>>> csvMap = datasetUtilities.readTaxomyCSV(request.getTaxonomyName(), false);
    /*
     * Costruisco la struttura delle folder utilizzando la mappa creata leggendo il csv.
     * ritornando una mappa contente i path per ogni categoria wikipedia.
     */
    Map<String, List<List<String>>> pathMap = datasetUtilities.createStructureFolder(csvMap, pathDataset);
    /*
     * Creo una mappa di classificazione basata sulla struttura delle folder appena create.
     * Che deve essere utilizzata dal classificatore python per poter riassociare alle label i nomi 
     * delle classi.
     */
    Map<String, List<String>> classificationMap = datasetUtilities.createMapForClassification(pathDataset);
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(classificationMapPath), classificationMap);
    System.out.println("Categories -> "+pathMap.size());
    /**
     * CORE PHASE.
     */
    int count = 0;
    Set<String> toExtract = new HashSet<String>();
    Map<String, EdgeResult> graph = dbGraph.getGraph("childs");
    for(String uriWiki : pathMap.keySet()){
      toExtract.add(uriWiki);     
      if(count%8 == 0 || count == pathMap.size()-1){
        System.out.println(((count*100)/pathMap.size())+"%");
        Map<String, Set<DocumentInfo>> results = new HashMap<String, Set<DocumentInfo>>();
        /*
         * Gather all the information completly Online
         */
        if(request.isDb())
          results = onlineDatasetTask(toExtract, 0, true, request.getLimitDocuments());
        /*
         * Gather the categories from database and the documents Online.
         */
        else
          results = databaseDatasetTask(toExtract,graph, request.getLimitDocuments());

        datasetUtilities.writeDocumentMap(pathMap, results);
        toExtract.clear();
      }
      count++;
    }
    graph.clear();

    return pathDataset;
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
  private Map<String,Set<DocumentInfo>> onlineDatasetTask(Set<String> categories,int maxLevel,boolean recursive,int limitDocs) throws IOException, InterruptedException, ExecutionException{
    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<DatasetTask>();
    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, maxLevel,recursive,limitDocs);
      datasetTasks.add(task);
    }
    List<Future<Map<String, Set<DocumentInfo>>>> result = pool.invokeAll(datasetTasks);
    Map<String,Set<DocumentInfo>> datasetMap = new HashMap<String, Set<DocumentInfo>>();
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
  private Map<String,Set<DocumentInfo>> databaseDatasetTask(Set<String> categories,Map<String, EdgeResult> graph,int limitDocs) throws InterruptedException, ExecutionException{
    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<DatasetTask>();
    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, graph, limitDocs);
      datasetTasks.add(task);
    }
    List<Future<Map<String, Set<DocumentInfo>>>> result = pool.invokeAll(datasetTasks);
    Map<String,Set<DocumentInfo>> datasetMap = new HashMap<String, Set<DocumentInfo>>();
    for(Future<Map<String, Set<DocumentInfo>>> future : result){
      datasetMap.putAll(future.get());
    }
    return datasetMap;
  }

  public ArrayList<String>  formatDataset(Set<String> pathList,String basePathDst,String basePathSrc, List<String> fileList, int numSourceToCopy, List<String> added,String datasetType, int minCut, int maxCut) throws IOException{
    // PER OGNNI PATH CALCOLATO
    ArrayList<String> addedToReturn = new ArrayList<String>();
    FileWriter writerLabelsTest = new FileWriter(new File(basePathSrc+"labelsItemTestWithOrigin.csv"));
    writerLabelsTest.write("id,origin,firstLabel,secondLabel,thirdLabel\n");

    for(String path:pathList){
      //creo la folder 
      new File(basePathDst+path).mkdir();
      Set<String> documentContainers = new HashSet<String>();
      // CALCOLO LA LISTA DI TUTTE LE CATEGORIE FOGLIA
      for(String file: fileList){
        file = file.replace("\\", "/").replace(basePathSrc, "");
        String [] splitted = file.split("/");
        if(file.contains(path.replace("root/", ""))){
          String toSave = file.replace("/"+splitted[splitted.length-1], "");
          documentContainers.add(toSave);
        }
      }
      // A QUESTO PUNTO AGGIUNGO I DOCUMENTI AL PATH CORRENTE USANDO LE CATEGORIE FOGLIA CALCOLATE
      int numSource = (numSourceToCopy/documentContainers.size());

      //Per ogni foglia della lista delle categorie foglia
      for(String documentContainer : documentContainers){

        String [] leafSplitted = documentContainer.split("/");
        String nameDocument = leafSplitted[leafSplitted.length-1];

        int count = 0; 
        // leggo il path del file corrente

        List<String> filesContained = datasetUtilities.listAllFiles(basePathSrc+documentContainer, new ArrayList<String>());
        for(String file : filesContained){
          file = file.replace("\\", "/");
          String[] splittedFileName = file.split("/");
          File f1 = new File(file);

          int wordCount=0;
          // conto il numero di parole del documento
          try(Scanner sc = new Scanner(new FileInputStream(file))){
            while(sc.hasNext()){
              sc.next();
              wordCount++;
            }
          }
          catch(Exception e){
            e.printStackTrace();
          }


          if(!added.contains(splittedFileName[splittedFileName.length-1])){
            if(count>=numSource)
              break;
            else{
              count++;
              File f2 = null;
              if(datasetType.equals("training")){
                addedToReturn.add(splittedFileName[splittedFileName.length-1]);
                f2 = new File(basePathDst+path+"/"+nameDocument+"_"+splittedFileName[splittedFileName.length-1]);
                FileUtils.copyFile(f1, f2);
              }
              else if(datasetType.equals("test") && (!addedToReturn.contains(splittedFileName[splittedFileName.length-1]))){  
                if(wordCount >=minCut && wordCount<=maxCut){
                  addedToReturn.add(splittedFileName[splittedFileName.length-1]);
                  f2 = new File(basePathDst+"/"+splittedFileName[splittedFileName.length-1]); 
                  //Codice per cercare le categorie dal grafo wikipedia. Creare un file CSV che contiene le categorie che il grafo ha restituito
                  List<String> labels =  analyzerWikipedia.getDocumentLabelsTaxonomy(splittedFileName[splittedFileName.length-1],true);
                  // se il nome della foglia è uguale al nome della labels in posizione 0. Faccio questo per prendere solo i documenti per i quali il grafo wikipedia ha una buona corrispondenza con l'etichetta usata durante la generazione del dataset
                  if(labels!=null && !labels.isEmpty()){
                    //if(nameLeaf.toLowerCase().equals(labels.get(0).toLowerCase())){
                    FileUtils.copyFile(f1, f2);
                    datasetUtilities.saveLabelsOnCSV(nameDocument,labels,writerLabelsTest,splittedFileName);
                    //} 
                  }
                }
              }
            }
          }
        }
      }

    }
    writerLabelsTest.flush();
    writerLabelsTest.close();
    return addedToReturn;
  }










}

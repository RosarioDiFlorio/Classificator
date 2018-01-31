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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.dataset.utility.DatasetRequest;
import eu.innovation.engineering.dataset.utility.DatasetResponse;
import eu.innovation.engineering.dataset.utility.DatasetTask;
import eu.innovation.engineering.dataset.utility.DatasetUtilities;
import eu.innovation.engineering.dataset.utility.DocumentInfo;
import eu.innovation.engineering.dataset.utility.WikiRequest;
import eu.innovation.engineering.graph.main.AnalyzerGraphWikipedia;
import eu.innovation.engineering.persistence.EdgeResult;
import eu.innovation.engineering.persistence.SQLiteWikipediaGraph;


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
    request.setLimitDocuments(10);
    request.setName("datasets_tassonomia_dijstra");
    request.setOnline(true);
    request.setTaxonomyCSV(new File("wheesbee_taxonomy.csv"));
    request.setTraining(true);
    request.setTest(true);
    request.setDb(true);
    /*
     * Call the method to build the dataset.
     */
    DatasetBuilder builder = new DatasetBuilder();
    builder.buildDataset(request);
  }

  @Override
  public DatasetResponse buildDataset(DatasetRequest request) {
    String basePath = "data/"+request.getName()+"/";
    new File(basePath).mkdirs();
    basePath = new File(basePath).getAbsolutePath()+"\\";
    System.out.println(basePath);
    basePath = basePath.replace("\\", "/");
    String pathDataset = basePath+"dataset";
    String basePathSrc = pathDataset;
    
    if(request.isOnline())
     basePathSrc =  WikipediaDataset(basePath,pathDataset,request);
  
    if(request.isTraining() || request.isTest()){      
      int numSourceToCopy = request.getLimitDocuments();    
      List<String> fileList = DatasetUtilities.listAllFiles(basePathSrc, new ArrayList<String>());
      Set<String> pathSet = DatasetUtilities.getAllPaths(basePathSrc);
      List<String> added = new ArrayList<>();
      if(request.isTraining()){
        String basePathDstTraining = basePath+"datasets_training/";
        new File(basePathDstTraining).mkdir();
        try {
          if(request.isTest())
            numSourceToCopy = numSourceToCopy -50;
          added = formatDataset(pathSet, basePathDstTraining, basePathSrc, fileList,numSourceToCopy, new ArrayList<String>(), request);
        }
        catch (IOException e) {
          e.printStackTrace();
        }
  
      }
      if(request.isTest()){
        numSourceToCopy = 50;
        String basePathDstTest = basePath+"datasets_test/";
        new File(basePathDstTest).mkdir();
        try {
          formatDataset(pathSet, basePathDstTest, basePathSrc, fileList,numSourceToCopy,added,request);
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
  
  
  
  
  
  
    }
  
    return null;
  }

  public static String WikipediaDataset(String basePath, String pathDataset, DatasetRequest request){
    /**
     * INIT DATASET.
     **/    
  
  
    String classificationMapPath = basePath+"categories.json";
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
      Map<String, List<String>> classificationMap = DatasetUtilities.createMapForClassification(pathDataset);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(classificationMapPath), classificationMap);
      System.out.println("Categories -> "+pathMap.size());
      /**
       * CORE PHASE.
       */
      int count = 0;
      Set<String> toExtract = new HashSet<>();
      SQLiteWikipediaGraph graphConnector = new SQLiteWikipediaGraph("databaseWikipediaGraph.db");
      Map<String, EdgeResult> graph = graphConnector.getGraph("childs");
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
  
          DatasetUtilities.writeDocumentMap(pathMap, results);
          toExtract.clear();
        }
        count++;
      }
    }
    catch (IOException | InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
  private static Map<String,Set<DocumentInfo>> onlineDatasetTask(Set<String> categories,int maxLevel,boolean recursive,int limitDocs) throws IOException, InterruptedException, ExecutionException{
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
  private static Map<String,Set<DocumentInfo>> databaseDatasetTask(Set<String> categories,Map<String, EdgeResult> graph,int limitDocs) throws InterruptedException, ExecutionException{
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

  public static ArrayList<String>  formatDataset(Set<String> pathList,String basePathDst,String basePathSrc, List<String> fileList, int numSourceToCopy, List<String> added, DatasetRequest request) throws IOException{
    // PER OGNNI PATH CALCOLATO
    ArrayList<String> addedToReturn = new ArrayList<String>();
    FileWriter writerLabelsTraining = new FileWriter(new File("labelsItemTrainingWithOrigin.csv"));
    FileWriter writerLabelsTest = new FileWriter(new File("labelsItemTestWithOrigin.csv"));

    writerLabelsTest.write("id,origin,firstLabel,secondLabel,thirdLabel\n");

    for(String path:pathList){

      //creo la folder 
      new File(basePathDst+path).mkdir();
      Set<String> leafList = new HashSet<String>();
      // CALCOLO LA LISTA DI TUTTE LE CATEGORIE FOGLIA
      for(String file: fileList){
        file = file.replace("\\", "/").replace(basePathSrc, "");
        String [] splitted = file.split("/");
        if(file.contains(path.replace("root/", ""))){
          String toSave = file.replace("/"+splitted[splitted.length-1], "");
          toSave = toSave.replace(basePathSrc,"");
          leafList.add(toSave);
        }
      }
      // A QUESTO PUNTO AGGIUNGO I DOCUMENTI AL PATH CORRENTE USANDO LE CATEGORIE FOGLIA CALCOLATE
      int numSource = (numSourceToCopy/leafList.size());
      AnalyzerGraphWikipedia analyzerWikipedia = new AnalyzerGraphWikipedia();
      //Per ogni foglia della lista delle categorie foglia
      for(String leaf:leafList){

        String [] leafSplitted = leaf.split("/");
        String nameLeaf = leafSplitted[leafSplitted.length-1];

        int count = 0; 
        // leggo il path del file corrente
        List<String> files = DatasetUtilities.listAllFiles(basePathSrc+"/"+leaf, new ArrayList<String>());
        for(String file:files){
          file = file.replace("\\", "/");
          String[] splitted = file.split("/");
          File f1 = new File(file);

          int wordCount=0;
          // conto il numero di parole del documento
          try(Scanner sc = new Scanner(new FileInputStream(file))){
            while(sc.hasNext()){
              sc.next();
              wordCount++;
            }
          }
          catch(Exception e){}


          if(!added.contains(splitted[splitted.length-1])){
            if(count>=numSource)
              break;
            else{
              count++;
              File f2 = null;
              if(request.isTraining()){
                addedToReturn.add(splitted[splitted.length-1]);
                f2 = new File(basePathDst+path+"/"+nameLeaf+"_"+splitted[splitted.length-1]);
                FileUtils.copyFile(f1, f2); 

                //Dataset To evaluate classifier with training set
                //File f3 = new File(basePathDst+"/datasetToEvaluate/"+splitted[splitted.length-1]); 
                //FileUtils.copyFile(f1, f3); 
                //List<String> labels =  analyzerWikipedia.getDocumentLabelsTaxonomy(splitted[splitted.length-1]);
                //saveLabelsOnCSV(nameLeaf,labels,writerLabelsTraining,splitted);
                /////////////////////////////////////////////////
              }
              if(request.isTest() && (!addedToReturn.contains(splitted[splitted.length-1]))){                           
                if(wordCount >=request.getMinCut() && wordCount<=request.getMaxCut()){
                  addedToReturn.add(splitted[splitted.length-1]);
                  f2 = new File(basePathDst+"/"+splitted[splitted.length-1]); 
                  //Codice per cercare le categorie dal grafo wikipedia. Creare un file CSV che contiene le categorie che il grafo ha restituito
                  List<String> labels =  analyzerWikipedia.getDocumentLabelsTaxonomy(splitted[splitted.length-1],true);
                  // se il nome della foglia è uguale al nome della labels in posizione 0. Faccio questo per prendere solo i documenti per i quali il grafo wikipedia ha una buona corrispondenza con l'etichetta usata durante la generazione del dataset
                  if(labels!=null && !labels.isEmpty()){
                    //if(nameLeaf.toLowerCase().equals(labels.get(0).toLowerCase())){
                    FileUtils.copyFile(f1, f2);
                    DatasetUtilities.saveLabelsOnCSV(nameLeaf,labels,writerLabelsTest,splitted);
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

package eu.innovation.engineering.graph.main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.innovation.engineering.api.WikipediaAPI;
import eu.innovation.engineering.dataset.utility.FilesUtilities;
import eu.innovation.engineering.graph.utility.Word2Vec;
import eu.innovation.engineering.persistence.EdgeResult;
import eu.innovation.engineering.persistence.SQLiteVectors;
import eu.innovation.engineering.persistence.SQLiteWikipediaGraph;






public class CrawlerGraphWikipedia {
  private static SQLiteWikipediaGraph dbGraph = new SQLiteWikipediaGraph("databaseWikipediaGraph.db");
  private static SQLiteVectors dbVectors = new SQLiteVectors("databaseVectors.db");
  private static Word2Vec word2vec = new Word2Vec();




  public static void main(String args[]) throws IOException, InterruptedException, ExecutionException{

    //    mainToBuildWeighedGraph(args);
    buildGraph(true);

  }


  /**
   * used to build wikipedia category graph
   * @param args
   * @throws IOException
   */
  public static void buildGraph(boolean isWeighted) throws IOException{
    dbGraph.setAutoCommit(false);
    dbVectors.setAutoCommit(false);
    try {
      HashSet<String> categories = new HashSet<String>(); 
      categories.add("Contents");
      BackupBFS(categories);
    }
    finally {
      WikipediaAPI.executorShutDown();
    }
    dbGraph.insertAndUpdateMarkedNodes(FilesUtilities.returnCategoriesFromTaxonomyCSV("wheesbee_taxonomy.csv"));
    Map<String, EdgeResult> graph = dbGraph.getGraph("parents");
    if (isWeighted) {      
      Set<String> vertexWikipedia = new HashSet<>(graph.keySet());
      graph.keySet().forEach(key->graph.get(key).getLinkedVertex().forEach(vertex->vertexWikipedia.add(vertex.getVertexName())));
      System.out.println(vertexWikipedia.size());
      try {
        saveVectorsWikipediaInDB(vertexWikipedia);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      for(String destination:graph.keySet()){
        insertWeightedEdges(destination, graph.get(destination).getLinkedVertex().stream().map(vertex->vertex.getVertexName()).collect(Collectors.toSet()));
      }
    }
    dbGraph.setAutoCommit(true);
    dbVectors.setAutoCommit(true);
  }

  public static void BackupBFS(Set<String> categories) throws JsonParseException, JsonMappingException, IOException{
    try{
      
      Map<String, EdgeResult> graph = dbGraph.getGraph("parents");
      Set<String> marked = new HashSet<String>(graph.keySet());
      Set<String> toVisit = dbGraph.getNamesFromMarkedNodes();
      graph.keySet().forEach(key->graph.get(key).getLinkedVertex().forEach(vertex->toVisit.add(vertex.getVertexName())));
      toVisit.removeAll(graph.keySet());
      toVisit.removeAll(dbGraph.getNamesFromEdges());
      System.out.println("toVisit->"+toVisit.size());
//      toVisit.forEach(str->System.out.println(str.contains("Category:")));
      
      
      
      graph.clear();
      BFS(categories, marked,new PriorityQueue<>(toVisit));
    }catch (Exception e) {
      e.printStackTrace();
      System.out.println("Something went wrong");
    }
  }


  /**
   * @param category
   * @param markedNode contains marked nodes already visited
   * @param adjacencylist Adjacency List
   * @return
   * @throws IOException
   * @throws ExecutionException 
   * @throws InterruptedException 
   */
  public static void BFS(Set<String> categoryList,Set<String> markedNode,PriorityQueue<String> vertexToVisit) throws IOException, InterruptedException, ExecutionException{
    int numConcurrency = Runtime.getRuntime().availableProcessors();
    Set<String> markedInsert = dbGraph.getNamesFromMarkedNodes();

    // Aggiungo a vertexToVisit i vertici da visitare. Serve perchè al primo lancio bisogna salvare il primo vertice in vertextovisit
    vertexToVisit.addAll(categoryList);

    // counter usato per la persistenza
    int countToPersiste = 0;

    // counter usato per la concorrenza
    int countToAddQuery = 0;

    // while there are vertex to visit, build adyacency list
    HashSet<String> categories = new HashSet<String>(categoryList);

    // finchè esistono vertici da visitare
    while(!vertexToVisit.isEmpty()){
      while(countToAddQuery < numConcurrency && (!vertexToVisit.isEmpty()) && categories.size()< numConcurrency){
        String vertex = vertexToVisit.poll();
        categories.add(vertex);
        countToAddQuery++;
        countToPersiste++;
        if(vertexToVisit.isEmpty())
          break;
      }
      countToAddQuery = 0;  

      HashMap<String, HashSet<String>> parentsMap = WikipediaAPI.getParentsRequest(categories);
      HashMap<String, HashSet<String>> childsMap = WikipediaAPI.getChildsRequest(categories);
      
      Set<String> toCheck  = new HashSet<>(categories);
      parentsMap.keySet().forEach(key->toCheck.addAll(parentsMap.get(key)));
      childsMap.keySet().forEach(key->toCheck.addAll(childsMap.get(key)));
      insertAndCheckMarkedNode(toCheck, markedInsert);

      for(String destination : parentsMap.keySet()){
        /*
         * per ognuno dei sui nodi parent inserisco un edge.
         */
        parentsMap.get(destination).forEach(source->{
          try {
            dbGraph.insertEdge(source, destination, 0);
          }
          catch (SQLException e) {
            System.out.println("Edge already contained");
          }
        });
        /*
         * inserisco alla lista dei nodi visitati
         */
        markedNode.add(destination);
      }


      // Creo un HashSet di appoggio per salvare parents e childs da aggiungere a vertexToVIsit
      HashSet<String> app = new HashSet<String>();
      for(String key : parentsMap.keySet()){
        app.addAll(parentsMap.get(key));
      }
      for(String key : childsMap.keySet()){
        app.addAll(childsMap.get(key));
      }
      for(String v : app){
        if(!markedNode.contains(v) && !vertexToVisit.contains(v)){
          vertexToVisit.add(v);
        }
      }
      // Persistenza
      if((countToPersiste >= 1000) || (vertexToVisit.isEmpty())){
        System.out.println(vertexToVisit.size());
        dbGraph.commitConnection();
        countToPersiste = 0;
      }
      /*
       * Svuoto il set di categorie che utilizzo per le query.
       */
      categories.clear();
      categoryList.clear();
    }
    dbGraph.commitConnection();
  }

  /**
   * This method return the vector's map created from the graph of the wikipedia's category.
   * If the map doesn't exit yet,  build the map from scratch Otherwise, load the map from the file (specified into the variable pathfile).
   * If the file exist load the map from the file and continue building it.
   * When there is no more category to convert in vectors the method return the map with the vectors.
   * @param vertexWikipedia 
   * @param pathFile
   * @return
   * @throws IOException
   * @throws InterruptedException 
   */
  public static void saveVectorsWikipediaInDB(Set<String> vertexWikipedia) throws IOException, InterruptedException{
    //carico le stopword dal file specificato.
    dbVectors.setAutoCommit(false);
    try{
      if(word2vec == null)
        word2vec = new Word2Vec();
      List<List<String>> toVectorize = new ArrayList<>();
      Set<String> donealready = dbVectors.getNamesVector();
      System.out.println("already done -> "+donealready.size());
      vertexWikipedia.removeAll(donealready);
      System.out.println("to do -> "+vertexWikipedia.size());

      //converto l'insieme di elementi da vettorizzare in una lista in modo da poterci accedere con l'indice.
      List<String> vertexList = new ArrayList<>(vertexWikipedia);

      Map<String,float[]> vectors = new HashMap<>();
      int offset = 0;//variabile che mi tiene traccia dell'indice corrente.
      for(int i = 0; i<vertexList.size();i++){

        //pulisco i nomi dagli underscore e dalle stop word
        toVectorize.add(word2vec.cleanText(vertexList.get(i)));
        //ogni tot di vertici eseguo la query al servizio Word2Vec
        if((toVectorize.size() == 200 || i == vertexWikipedia.size()-1)){
          float[][] vectorizedNames = word2vec.returnVectorsFromTextList(toVectorize);
          for (int j = 0; j < toVectorize.size(); j++) {
            vectors.put(vertexList.get(j + offset).replace(" ", "_"), vectorizedNames[j]);
          }
          offset += toVectorize.size();
          dbVectors.insertVectors(vectors);
          if(offset % 100000 == 0 || i == vertexWikipedia.size()-1)
            dbVectors.commitConnection();
          vectors.clear();
          toVectorize.clear();
        }
      }
    }finally {
      dbVectors.commitConnection();
    }
  }
  
  
  private static void insertAndCheckMarkedNode(Set<String> toCheck, Set<String> alreadyInsert){
    toCheck.stream().map(el-> el = el.replace(" ", "_")).filter(el->!alreadyInsert.contains(el)).forEach(el->{
      dbGraph.insertMarkedNode(el, false);
      alreadyInsert.add(el);
    });
    //    dbGraph.commitConnection();
  }


  private static void insertWeightedEdges(String destination, Set<String> sources){
    float[] destinationVector = dbVectors.getVectorByName(destination);
    for(String source : sources){
      double weight = 0;
      float[] sourceVector = dbVectors.getVectorByName(source);
      if(validateVector(destinationVector) && validateVector(sourceVector))
        weight = cosineSimilarityInverse(destinationVector, sourceVector); 
      else{
        weight =  (3.14/2);
      }    
        dbGraph.updateEdge(source, destination, weight);
    }
  }


  /**
   * this method check if a vector is different from 0 vector
   * @param vector
   * @return
   */
  private static boolean validateVector(float[] vector){
  
    if (vector == null)
      return false;
  
    for(int i=0;i<vector.length-1;i++){
      if (vector[i]!=0.0)
        return true;
    }
    return false;
  }


  /**
   * Used to do inverse of cosine similarity with 2 vector
   * @param vectorA
   * @param vectorB
   * @return
   */
  public static double cosineSimilarityInverse(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
      for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += vectorA[i] * vectorA[i];
        normB += vectorB[i] * vectorB[i];
      }   
    }

    if(dotProduct == 0 || (normA * normB) == 0){
      return  0.001;

    }
    else{
      Double toReturn = Math.acos((dotProduct) / (Math.sqrt(normA * normB)));

      if (toReturn.isNaN())
        return 0.001;

      return Math.acos((dotProduct) / (Math.sqrt(normA * normB)))+0.001;
    }
  }


  public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
      for (int i = 0; i < vectorA.length; i++) {
        dotProduct += vectorA[i] * vectorB[i];
        normA += vectorA[i] * vectorA[i];
        normB += vectorB[i] * vectorB[i];
      }   
    }

    if(dotProduct == 0 || (normA * normB) == 0){
      return 0;

    }
    else{
      return (dotProduct) / (Math.sqrt(normA * normB));
    }
  }







}

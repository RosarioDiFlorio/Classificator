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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.innovation.engineering.api.WikipediaAPI;
import eu.innovation.engineering.dataset.utility.DatasetUtilities;
import eu.innovation.engineering.graph.utility.Edge;
import eu.innovation.engineering.graph.utility.Word2Vec;
import eu.innovation.engineering.persistence.DbApplication;
import eu.innovation.engineering.persistence.EdgeResult;
import eu.innovation.engineering.services.GraphRequest;
import eu.innovation.engineering.services.GraphResponse;
import eu.innovation.engineering.services.WikiGraphRequest;






public class CrawlerGraphWikipedia extends DbApplication implements WikiGraphRequest{

  private static Word2Vec word2vec = new Word2Vec();

  /**
   * EXAMPLE AND TEST MAIN
   * @param args
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static void main(String args[]) throws IOException, InterruptedException, ExecutionException{
    buildGraph(true);
  }



  @Override
  public GraphResponse buildGraph(GraphRequest request) {
    GraphResponse response = new GraphResponse();
    try {
      buildGraph(request.isWeighted());
    }
    catch (IOException e) {
      response.setStatus(500);
      response.setMessage("Error in the graph creation\n"+e.getMessage());
      return response;
    }
    response.setStatus(200);
    response.setMessage("Graph creation completed");    
    return response;
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
      backupBFS(categories);
    }
    finally {
      dbGraph.insertAndUpdateMarkedNodes(DatasetUtilities.returnCategoriesFromTaxonomyCSV("wheesbee"));
      dbGraph.commitConnection();
      WikipediaAPI.executorShutDown();
    }    
    Map<String, EdgeResult> graph = dbGraph.getGraph("parents");
    if (isWeighted) {      
      Set<String> vertexWikipedia = new HashSet<String>(graph.keySet());
      graph.keySet().forEach(key->graph.get(key).getLinkedVertex().forEach(vertex->vertexWikipedia.add(vertex.getVertexName())));
      System.out.println(vertexWikipedia.size());
      try {
        saveVectorsWikipediaInDB(vertexWikipedia);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }finally {
        dbVectors.commitConnection();
        dbGraph.commitConnection();
      }
      updateWeightEdges(dbGraph.getListEdgesByDistance(0));
    }
    dbGraph.setAutoCommit(true);
    dbVectors.setAutoCommit(true);
  }


  public static void backupBFS(Set<String> categories) throws JsonParseException, JsonMappingException, IOException{
    try{

      Map<String, EdgeResult> graphParents = dbGraph.getGraph("parents");
      Set<String> marked = new HashSet<String>(dbGraph.getNamesFromEdges());
      Set<String> toVisit = dbGraph.getNamesFromMarkedNodes();
      graphParents.keySet().forEach(key->graphParents.get(key).getLinkedVertex().forEach(vertex->toVisit.add(vertex.getVertexName())));
      graphParents.clear();

      toVisit.removeAll(marked);  
      System.out.println("toVisit->"+toVisit.size());
      BFS(categories, marked,new PriorityQueue<String>(toVisit));
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
    int numConcurrency = 20;
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

      Map<String, Set<String>> parentsMap = WikipediaAPI.getParentsRequest(categories);
      Map<String, Set<String>> childsMap = WikipediaAPI.getChildsRequest(categories);

      
      Set<String> toCheck  = new HashSet<String>(categories);
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
  private static void saveVectorsWikipediaInDB(Set<String> vertexWikipedia) throws IOException, InterruptedException{
    //carico le stopword dal file specificato.
    dbVectors.setAutoCommit(false);
    try{
      if(word2vec == null)
        word2vec = new Word2Vec();
      List<List<String>> toVectorize = new ArrayList<List<String>>();
      Set<String> donealready = dbVectors.getNamesVector();
      System.out.println("already done -> "+donealready.size());
      vertexWikipedia.removeAll(donealready);
      System.out.println("to do -> "+vertexWikipedia.size());

      //converto l'insieme di elementi da vettorizzare in una lista in modo da poterci accedere con l'indice.
      List<String> vertexList = new ArrayList<String>(vertexWikipedia);

      Map<String,float[]> vectors = new HashMap<String, float[]>();
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
  }


  private static void updateWeightEdges(List<Edge> edgeList){
    for(Edge e:edgeList){
      double weight = 0;
      float[] destinationVector = dbVectors.getVectorByName(e.getChilds());
      float[] sourceVector = dbVectors.getVectorByName(e.getParents());
      if(validateVector(destinationVector) && validateVector(sourceVector))
        weight = cosineSimilarityInverse(destinationVector, sourceVector); 
      else{
        weight =  (3.14/2);
      }
      dbGraph.updateEdge(e.getParents(), e.getChilds(), weight);


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

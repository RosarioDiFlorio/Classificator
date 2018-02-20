package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.innovationengineering.solrclient.auth.collection.queue.UpdatablePriorityQueue;
import persistence.EdgeResult;
import persistence.SQLiteVectors;
import persistence.SQLiteWikipediaGraph;
import utility.AdjacencyListRow;
import utility.PathInfo;
import utility.StopWordEnglish;
import utility.Word2Vec;



public class AnalyzerWikipediaGraph {

  private static SQLiteWikipediaGraph graphConnector = new SQLiteWikipediaGraph("databaseWikipediaGraph.db");
  private static HashMap<String,Set<String>> mappingTaxonomyWikipedia = null;
  private static Word2Vec word2Vec;
  private static Map<String,EdgeResult> graph;


  public static void main(String[] args) throws IOException{
    Set<String> debug = graphConnector.getMarkedNodes();
    System.out.println(debug.size());
    graphConnector.setAutoCommit(false);
    //7674809 tomografia.
    //9912937 museo della grappa.
    System.out.println(getDocumentLabelsTaxonomy("9912937", true));
    graphConnector.setAutoCommit(true);
  }

  /**
   * @param args
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws InterruptedException
   * @deprecated 
   */
  @Deprecated
  public static void mainToBuildVectors(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException{
    HashMap<String, AdjacencyListRow> adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipediaCleared");
    Set<String> toVectorize = new HashSet<String>(adjacencyList.keySet());
    for(String key: adjacencyList.keySet()){
      toVectorize.addAll(adjacencyList.get(key).getLinkedVertex());  
    }
    saveVectorsWikipediaInDB(toVectorize,"databaseVectors.db");
  }

  /**
   * @param pathFile
   * @return
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @deprecated
   */
  @Deprecated
  public static Map<String,float[]> loadVectorsWikipediaGraph(String pathFile) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(new File(pathFile), new TypeReference<Map<String,float[]>>() {});
  }




  public static List<String> cleanText(String text){
    StopWordEnglish stopWords = new StopWordEnglish("stopwords_en.txt");
    text = text.replaceAll("\\p{Punct}", " ");
    text = text.replaceAll("\\d+", " ");
    return Arrays.asList(text.split(" ")).stream().filter(el->!stopWords.isStopWord(el) && !el.matches("")).map(el->el.toLowerCase().trim()).collect(Collectors.toList());
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
  public static void saveVectorsWikipediaInDB(Set<String> vertexWikipedia,String dbName) throws IOException, InterruptedException{
    //carico le stopword dal file specificato.
    StopWordEnglish stopWords = new StopWordEnglish("stopwords_en.txt");
    SQLiteVectors sql = new SQLiteVectors(dbName);
    sql.setAutoCommit(false);
    try{
      if(word2Vec == null)
        word2Vec = new Word2Vec();
      List<List<String>> toVectorize = new ArrayList<>();
      Set<String> donealready = sql.getNamesVector();
      System.out.println("already done -> "+donealready.size());
      vertexWikipedia.removeAll(donealready);
      System.out.println("to do -> "+vertexWikipedia.size());

      //converto l'insieme di elementi da vettorizzare in una lista in modo da poterci accedere con l'indice.
      List<String> vertexList = new ArrayList<>(vertexWikipedia);

      Map<String,float[]> vectors = new HashMap<>();
      int offset = 0;//variabile che mi tiene traccia dell'indice corrente.
      for(int i = 0; i<vertexList.size();i++){

        //pulisco i nomi dagli underscore e dalle stop word
        toVectorize.add(cleanText(vertexList.get(i)));
        //ogni tot di vertici eseguo la query al servizio Word2Vec
        if((toVectorize.size() == 200 || i == vertexWikipedia.size()-1)){
          float[][] vectorizedNames = word2Vec.returnVectorsFromTextList(toVectorize);
          for (int j = 0; j < toVectorize.size(); j++) {
            vectors.put(vertexList.get(j + offset).replace(" ", "_"), vectorizedNames[j]);
          }
          offset += toVectorize.size();
          sql.insertVectors(vectors);
          if(offset % 100000 == 0 || i == vertexWikipedia.size()-1)
            sql.commitConnection();
          vectors.clear();
          toVectorize.clear();
        }
      }
    }finally {
      sql.commitConnection();
      System.exit(0);
    }
  }


  public static List<String> getDocumentLabelsTaxonomy(String idDocument,boolean withDijstra) throws IOException{
    if(mappingTaxonomyWikipedia == null){
      mappingTaxonomyWikipedia = new HashMap<>();
      Map<String, List<String>> taxonomyCsv = getTaxonomyCSV("wheesbee_taxonomy.csv");
      for(String wikiCat: taxonomyCsv.keySet()){
        Set<String> toAdd = new HashSet<>();
        toAdd.add(taxonomyCsv.get(wikiCat).get(taxonomyCsv.get(wikiCat).size()-1));
        if(!mappingTaxonomyWikipedia.containsKey(wikiCat))
          mappingTaxonomyWikipedia.put(wikiCat, toAdd);
        else
          mappingTaxonomyWikipedia.get(wikiCat).addAll(toAdd);
      }
    }
    List<String> toReturn = new ArrayList<>();
    List<String> wikipediaLabels = new ArrayList<>();
    wikipediaLabels = getListLabels(idDocument, withDijstra);
    for(String label: wikipediaLabels){
      if(mappingTaxonomyWikipedia.containsKey(label))
        toReturn.addAll(mappingTaxonomyWikipedia.get(label));
    }
    return toReturn;
  }



  public static double getAVG(PathInfo p,double avg,double d){
    if(p!= null){
      avg += p.getInverseCosine();
      getAVG(p, avg, d);
    }
    return avg/d;
  }

  public static double getVariance(PathInfo p,double variance,double avg,double d){
    if(p!=null){
      variance += Math.pow((p.getInverseCosine()-avg),2);
      getVariance(p, variance,avg,d);
    }
    return variance/d;
  }

  public static void getPath(PathInfo p, StringBuilder builder){
    if (p != null) {
      builder.insert(0, "/" + p.getName()+"["+p.getInverseCosine()+"]");
      getPath(p.getParent(), builder);      
    }
  }

  public static List<String> getListLabels(String idDocument,boolean withDjistra) throws IOException{
    Set<String> documentCategories = getParentCategoriesByIdPage(idDocument);
    List<PathInfo> results = new ArrayList<PathInfo>();
    for(String category: documentCategories){
      Set<PathInfo> pathList;
      if(withDjistra)
        pathList = searchDjistraMarkedNode(category, 2);
      else
        pathList = seachBFSMarkedNodes(graphConnector, idDocument, 2);


      for(PathInfo p: pathList){
        if(results.contains(p)){
          PathInfo tmp = results.get(results.indexOf(p));
          if(p.getValue() < tmp.getValue()){
            results.remove(tmp);
            results.add(p);
          }
        }else
          results.add(p);
      }
    }
    List<PathInfo> orderedResults = new ArrayList<PathInfo>(results);
    Collections.sort(orderedResults);
    return orderedResults.stream().map(e->e.getName()).collect(Collectors.toList());
  }



  /**
   * Search the nearest n marked vertex starting to the vertex passed in input to this function.
   * Using the dijstra algorithm.
   * The weight for each vertex u is based on the inverseCosineSimiliraty between the vertex (u,v).
   * @param adjacencyList
   * @param vertexStartName
   * @param numberOfMarkedVertex
   * @param startValue
   * @return
   */
  public static Set<PathInfo> searchDjistraMarkedNode(String vertexStartName,int numberOfMarkedVertex){
    if(graph == null){
      System.out.println("Inizialing graph");
      graphConnector.setAutoCommit(false);
      graph = graphConnector.getGraph("parents");
      System.out.println("graph loaded -> "+graph.size());
      graphConnector.setAutoCommit(true);
    }
    //insieme di nodi marcati da ritornare.
    Set<PathInfo> nearestMarkedVertex = new HashSet<PathInfo>();
    Set<String> markedNodes = graphConnector.getMarkedNodes();
    int lenPath = 0; //conta la lunghezza del path
    int countMarked = 0; //conta il numero di nodi marcati trovati
    Set<PathInfo> visitedVertex = new HashSet<PathInfo>(); //lista dei nodi già visitati.
    /*
     * creo il nodo di partenza.
     */
    PathInfo startingPoint = new PathInfo(vertexStartName, 0); 
    startingPoint.setLenPath(lenPath); 
    /*
     * controllo se il nodo di partenza è una categoria contenuta all'interno della tassonomia.
     */
    if(markedNodes.contains(vertexStartName)){
      nearestMarkedVertex.add(startingPoint);
      countMarked++;
      if(countMarked >= numberOfMarkedVertex)
        return nearestMarkedVertex;
    }
    /*
     * DIJSTRA ALGORITHM
     * creo la priority queue.
     * ed aggiungo il nodo di partenza.
     */
    UpdatablePriorityQueue<PathInfo> q = new UpdatablePriorityQueue<>();
    q.add(startingPoint);
    while(!q.isEmpty()){
      if(countMarked >= numberOfMarkedVertex)
        return nearestMarkedVertex;
      /*
       * prendo l'elemento con il peso più minore.
       */
      PathInfo currentVertex = q.poll();
      if(graph.containsKey(currentVertex.getName())){
        EdgeResult edge = graph.get(currentVertex.getName());


        /*
         * controllo se il nodo di partenza è una categoria contenuta all'interno della tassonomia.
         */
        if(markedNodes.contains(currentVertex.getName())){
          nearestMarkedVertex.add(currentVertex);
          countMarked++;
          if(countMarked >= numberOfMarkedVertex)
            return nearestMarkedVertex;
        }
        visitedVertex.add(currentVertex); // aggiungo il nodo alla lista dei nodi visitati.
        /*
         * Per ognuno dei Nodi Linkati creo una lista di PathInfo.
         * con value uguale al valore del nodo corrente + valore del nodo linkato considerato.
         */
        List<PathInfo> linkedVertex = edge.getLinkedVertex().stream().map(v->new PathInfo(v.getVertexName(),(currentVertex.getValue()+v.getSimilarity()),v.getSimilarity())).collect(Collectors.toList());
        /*
         * elimino dalla coda i nodi linkati al nodo corrente che hanno valore più alto di quello appena considerato.
         */
        boolean updateQueue = false;
        Iterator<PathInfo> iter = q.iterator();
        while(iter.hasNext()){
          PathInfo p = iter.next();
          int index = linkedVertex.indexOf(p);
          if(index > 0){
            PathInfo tmp = linkedVertex.get(index);

            //update element into the priority queue
            if(p.getValue() > tmp.getValue()){
              p.setValue(tmp.getValue());
              updateQueue = true;              
            }
          }
        }
        if (updateQueue) {
          q.update();
        }

        /*
         * filtro i nodi linkati che non appartengo alla priority queue e non appartengo alla lista dei nodi visitati.
         * per ognuno di questi nodi assegno con parent il nodo corrente e gli aggiorno la lunghezza del path.
         * infine li aggiungo alla priority queue dei prossimi nodi da visitare.
         */
        linkedVertex.stream().filter(el->!q.contains(el) && !visitedVertex.contains(el)).peek(el->el.setParent(currentVertex)).peek(el->el.setLenPath(currentVertex.getLenPath()+1)).forEach(q::add);
      }
    }
    return nearestMarkedVertex;
  }



  /**
   * Search the nearest n marked vertex starting to the vertex passed in input to this function.
   * @param adjacencyList
   * @param vertexStart
   * @param numberOfMarkedVertex
   * @return
   * 
   */
  public static Set<PathInfo> seachBFSMarkedNodes(SQLiteWikipediaGraph graph,String vertexStart,int numberOfMarkedVertex){
    //insieme di nodi marcati da ritornare.
    Set<PathInfo> nearestMarkedVertex = new HashSet<PathInfo>();

    try{

      int lenghtPath = 0;
      PathInfo vertexStartInfo = new PathInfo(vertexStart,lenghtPath);
      //contatore del numero di nodi marcati trovati.
      int countMarked = 0;
      //controllo se il nodo di partenza è una categoria marcata.
      if(graph.isMarked(vertexStart)){
        nearestMarkedVertex.add(vertexStartInfo);
        countMarked++;
        if(countMarked >= numberOfMarkedVertex)
          return nearestMarkedVertex;
      }
      //lista dei nodi già visitati
      Set<PathInfo> visitedVertex = new HashSet<PathInfo>();
      //aggiungo il nodo di partenza alla lista dei nodi già visitati.
      visitedVertex.add(vertexStartInfo);
      //coda dei nodi da visitare
      LinkedList<PathInfo> vertexToVisit = new LinkedList<PathInfo>();
      //incremento la lunghezza del path per i nodi linkati dal nodo di partenza.
      lenghtPath++;

      EdgeResult edgeStart = graph.getEdgeList(vertexStart, "parents");
      ///!!!!!!!!!!!!!!!!! JAVA 8 FUNCTION (conversione di Set<String> in un Set<PathInfo>) !!!!!!!!!!!!!!!!
      //trasformo il set di stringhe linkate dal nodo in un set di oggetti PathInfo.
      Set<PathInfo> linkedVertex = edgeStart.getLinkedVertex().stream().map(vertex-> new PathInfo(vertex.getVertexName(), vertexStartInfo.getValue()+1)).collect(Collectors.toSet());

      //aggiungo tutti i nodi linkati dal nodo di partenza ai nodi da visitare.
      vertexToVisit.addAll(linkedVertex);
      vertexToVisit.stream().forEach(el->el.setParent(vertexStartInfo));
      //finchè i nodi da visitare non sono terminati.
      while(!vertexToVisit.isEmpty()){
        //prendo il primo elemento della coda.
        PathInfo vertex = vertexToVisit.poll();
        //aggiungo il nodo alla lista dei vertici già visitati.
        visitedVertex.add(vertex);
        lenghtPath ++;

        //se il nodo corrente è una categoria marcata l'aggiungo alla lista da ritornare.
        if(graph.isMarked(vertex.getName())){
          nearestMarkedVertex.add(vertex);
          countMarked++;
          if(countMarked >= numberOfMarkedVertex)
            return nearestMarkedVertex;
        }
        //aggiungo i prossimi nodi da visitare
        Set<String> nextToVisit = graph.getEdgeList(vertex.getName(), "parents").getLinkedVertex().stream().map(v->v.getVertexName()).collect(Collectors.toSet());
        for(String v : nextToVisit){
          PathInfo vInfo = new PathInfo(v, vertex.getValue()+1);
          vInfo.setParent(vertex);
          if(!visitedVertex.contains(vInfo) && !vertexToVisit.contains(vInfo))
            vertexToVisit.add(vInfo);
        }
      }
    }catch (SQLException e) {
      e.printStackTrace();
    }
    //ritorno la lista di nodi marcati.
    return nearestMarkedVertex;
  }


  /**
   * read Category by Taxonomy CSV. Input file contains all categories used from Taxonomy
   * @param csvFile
   * @param labeled
   * @return 
   * @return
   * @throws IOException
   */
  public static  Map<String,List<String>> getTaxonomyCSV(String csvFile) throws IOException{

    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<String>> dataMap = new HashMap<String, List<String>>();

    BufferedReader br = new BufferedReader(new FileReader(csvFile));

    while ((line = br.readLine()) != null) {
      // use comma as separator
      String[] csvData = line.split(cvsSplitBy); 
      List<String> data = new ArrayList<String>();
      if(csvData.length>=2){
        for(int i =0;i<csvData.length-1;i++){
          data.add(csvData[i].trim());
        }
        String key = csvData[csvData.length-1].trim().replace("en.wikipedia.org/wiki/Category:", "");
        if(!key.equals(""))
          dataMap.put(key, data);
      }
    }
    return dataMap;
  } 


  /**
   * Return the categories of a Wikipedia's page.
   * Take as input the id of a Wikipedia's document.
   * @param idDocument
   * @return
   * @throws IOException
   */
  public static Set<String> getParentCategoriesByIdPage(String idDocument) throws IOException{
    Set<String> toReturn  = new HashSet<>();
    JsonArray categoriesParent = null;
    String parentsURL = "https://en.wikipedia.org/w/api.php?action=query&pageids="+idDocument+"&prop=categories&clshow=!hidden&cllimit=500&indexpageids&format=json";
    JsonObject responseParent = CrawlerWikipediaCategory.getJsonResponse(parentsURL);
    //build ids array 
    JsonArray idsJsonArray = responseParent.get("query").getAsJsonObject().get("pageids").getAsJsonArray();
    ArrayList<String> ids = new ArrayList<String>();
    for (JsonElement e : idsJsonArray){
      if (Integer.parseInt(e.getAsString())>0){
        ids.add(e.getAsString());
      }
    }
    for(String id : ids){
      try{
        categoriesParent = responseParent.getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray();
        if(categoriesParent!=null){
          // add all vertex obtained to hashset
          for(JsonElement cat : categoriesParent){
            String name = cat.getAsJsonObject().get("title").getAsString();
            String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
            toReturn.add(namesplitted[1]);
          }
        }
      }
      catch(Exception e){
        System.out.println(id+": hasn't parents category --- URL: "+parentsURL);
      }
    }
    return toReturn;
  }

}

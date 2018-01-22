package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import persistence.SQLiteConnector;
import utility.AdjacencyListRow;
import utility.AdjacencyListRowVertex;
import utility.PathInfo;
import utility.StopWordEnglish;
import utility.Word2Vec;

public class AnalyzerWikipediaGraph {

  private static  HashMap<String, AdjacencyListRow> adjacencyList = null;
  private static HashMap<String,Set<String>> mappingTaxonomyWikipedia = null;
  private static Map<String,float[]> vectorsWikipediaVertex = null;
  private static Map<String,AdjacencyListRowVertex> graphWeighed = null;
  private static Word2Vec word2Vec;


  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException{
    mainToBuildVectors(args);
  }


  public static void mainToBuildVectors(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException{ 
    adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipediaCleared");
    Set<String> toVectorize = new HashSet<String>(adjacencyList.keySet());
    for(String key: adjacencyList.keySet()){
      toVectorize.addAll(adjacencyList.get(key).getLinkedVertex());  
    }
    getVectorsWikipediaGraph(toVectorize);
  }

  public static Map<String,float[]> loadVectorsWikipediaGraph(String pathFile) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(new File(pathFile), new TypeReference<Map<String,float[]>>() {});
  }


  private static List<String> cleanText(String text){
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
  public static void getVectorsWikipediaGraph(Set<String> vertexWikipedia) throws IOException, InterruptedException{
    //carico le stopword dal file specificato.
    StopWordEnglish stopWords = new StopWordEnglish("stopwords_en.txt");
    SQLiteConnector sql = new SQLiteConnector();

    if(word2Vec == null)
      word2Vec = new Word2Vec();
    List<List<String>> toVectorize = new ArrayList<>();
    Set<String> donealready = sql.getNamesVector();
    System.out.println("already done -> "+donealready.size());
    vertexWikipedia.removeAll(donealready);
    System.out.println("to do -> "+vertexWikipedia.size());

    //converto l'insieme di elementi da vettorizzare in una lista in modo da poterci accedere con l'indice.
    List<String> vertexList = new ArrayList<>(vertexWikipedia);
    int offset = 0;         //variabile che mi tiene traccia dell'indice corrente.
    for(int i = 0; i<vertexList.size();i++){
      
      //pulisco i nomi dagli underscore e dalle stop word
        toVectorize.add(cleanText(vertexList.get(i)));
        //ogni tot di vertici eseguo la query al servizio Word2Vec
        if((toVectorize.size() == 10 || i == vertexWikipedia.size()-1)){
          Map<String,float[]> vectors = new HashMap<>();
          float[][] vectorizedNames = word2Vec.returnVectorsFromTextList(toVectorize);
          for (int j = 0; j < toVectorize.size(); j++) {
            vectors.put(vertexList.get(j + offset).replace(" ", "_"), vectorizedNames[j]);
          }
          sql.insertVectors(vectors);
          offset += toVectorize.size();
          toVectorize.clear();
        }
      
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
    if(withDijstra)
      wikipediaLabels = getDocumentLabelsDijstra(idDocument);
    else
      wikipediaLabels = getDocumentLabelsBFS(idDocument);
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


  public static List<String> getDocumentLabelsDijstra(String idDocument) throws IOException{
    Set<String> documentCategories = getParentCategoriesByIdPage(idDocument);
    List<PathInfo> results = new ArrayList<PathInfo>();
    if(graphWeighed == null){
      ObjectMapper mapper = new ObjectMapper();
      graphWeighed = mapper.readValue(new File("graphWikipediaWeighed"), new TypeReference<Map<String,AdjacencyListRowVertex>>() {});
    }

    for(String category: documentCategories){

      for(PathInfo p: searchDjistraMarkedNode(graphWeighed, category, 2,0)){

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
    //riscalo i valori dividendoli per la lunghezza del path.  
    for(PathInfo p: results){
      p.setValue(p.getValue()/p.getLenPath());
    }
    List<PathInfo> orderedResults = new ArrayList<PathInfo>(results);
    Collections.sort(orderedResults);
    //DEBUG PRINT
    System.out.println(orderedResults); 
    for(PathInfo p: orderedResults){
      StringBuilder builder = new StringBuilder();
      getPath(p, builder);
      System.out.println(builder.toString());

    }
    return orderedResults.stream().map(e->e.getName()).collect(Collectors.toList());
  }



  public static List<String> getDocumentLabelsBFS(String idDocument) throws IOException{
    Set<String> documentCategories = getParentCategoriesByIdPage(idDocument);
    List<PathInfo> results = new ArrayList<PathInfo>();
    if(adjacencyList == null)    
      adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipediaCleared");
    for(String category: documentCategories){
      for(PathInfo p: seachBFSMarkedNodes(adjacencyList, category, 3)){
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
    //DEBUG PRINT
    /*System.out.println(orderedResults); 
    for(PathInfo p: orderedResults){
      StringBuilder builder = new StringBuilder();
      getPath(p, builder);
      System.out.println(builder.toString());
    }*/
    return orderedResults.stream().map(e->e.getName()).collect(Collectors.toList());
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
  public static Set<PathInfo> searchDjistraMarkedNode(Map<String,AdjacencyListRowVertex> adjacencyList,String vertexStartName,int numberOfMarkedVertex,double startValue){
    //insieme di nodi marcati da ritornare.
    Set<PathInfo> nearestMarkedVertex = new HashSet<PathInfo>();
    if(adjacencyList.containsKey(vertexStartName)){
      int lenPath = 0; //conta la lunghezza del path
      int countMarked = 0; //conta il numero di nodi marcati trovati
      Set<PathInfo> visitedVertex = new HashSet<PathInfo>(); //lista dei nodi già visitati.
      /*
       * creo il nodo di partenza.
       */
      PathInfo startingPoint = new PathInfo(vertexStartName, startValue); 
      startingPoint.setLenPath(lenPath); 
      /*
       * controllo se il nodo di partenza è una categoria contenuta all'interno della tassonomia.
       */
      if(adjacencyList.get(vertexStartName).isTaxonomyCategory()){
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
        /*
         * prendo l'elemento con il peso più minore.
         */
        PathInfo currentVertex = q.poll();
        if(adjacencyList.containsKey(currentVertex.getName())){
          /*
           * controllo se il nodo di partenza è una categoria contenuta all'interno della tassonomia.
           */
          if(adjacencyList.get(currentVertex.getName()).isTaxonomyCategory()){
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
          List<PathInfo> linkedVertex = adjacencyList.get(currentVertex.getName()).getLinkedVertex().stream().map(v->new PathInfo(v.getVertexName(),(currentVertex.getValue()+v.getSimilarity()),v.getSimilarity())).collect(Collectors.toList());       
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
                updateQueue = true;              }
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

    }
    return nearestMarkedVertex;
  }



  /**
   * Search the nearest n marked vertex starting to the vertex passed in input to this function.
   * @param adjacencyList
   * @param vertexStart
   * @param numberOfMarkedVertex
   * @return
   */
  public static Set<PathInfo> seachBFSMarkedNodes(Map<String,AdjacencyListRow> adjacencyList,String vertexStart,int numberOfMarkedVertex){
    //insieme di nodi marcati da ritornare.
    Set<PathInfo> nearestMarkedVertex = new HashSet<PathInfo>();

    if(adjacencyList.containsKey(vertexStart)){
      int lenghtPath = 0;
      PathInfo vertexStartInfo = new PathInfo(vertexStart,lenghtPath);
      //contatore del numero di nodi marcati trovati.
      int countMarked = 0;
      //controllo se il nodo di partenza è una categoria marcata.
      if(adjacencyList.get(vertexStart).isTaxonomyCategory()){
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

      ///!!!!!!!!!!!!!!!!! JAVA 8 FUNCTION (conversione di Set<String> in un Set<PathInfo>) !!!!!!!!!!!!!!!!
      //trasformo il set di stringhe linkate dal nodo in un set di oggetti PathInfo.
      Set<PathInfo> linkedVertex = adjacencyList.get(vertexStart).getLinkedVertex().stream().map(vertexName-> new PathInfo(vertexName, vertexStartInfo.getValue()+1)).collect(Collectors.toSet());

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
        if(adjacencyList.containsKey(vertex.getName())){
          //se il nodo corrente è una categoria marcata l'aggiungo alla lista da ritornare.
          if(adjacencyList.get(vertex.getName()).isTaxonomyCategory()){
            nearestMarkedVertex.add(vertex);
            countMarked++;
            if(countMarked >= numberOfMarkedVertex)
              return nearestMarkedVertex;
          }
          //aggiungo i prossimi nodi da visitare
          for(String v : adjacencyList.get(vertex.getName()).getLinkedVertex()){
            PathInfo vInfo = new PathInfo(v, vertex.getValue()+1);
            vInfo.setParent(vertex);
            if(!visitedVertex.contains(vInfo) && !vertexToVisit.contains(vInfo))
              vertexToVisit.add(vInfo);
          }
        }else // porzione aggiunta per possibile errore di formato delle chiavi all'interno della lista di adiacenze. 
          if(adjacencyList.containsKey(vertex.getName().replace("_", " "))){
            vertex.setName(vertex.getName().replace("_", " "));
            if(adjacencyList.get(vertex.getName()).isTaxonomyCategory()){
              nearestMarkedVertex.add(vertex);
              countMarked++;
              if(countMarked >= numberOfMarkedVertex)
                return nearestMarkedVertex;
            }
            //aggiungo i prossimi nodi da visitare
            for(String v : adjacencyList.get(vertex.getName()).getLinkedVertex()){
              PathInfo vInfo = new PathInfo(v, vertex.getValue()+1);
              if(!visitedVertex.contains(vInfo) && !vertexToVisit.contains(vInfo))
                vertexToVisit.add(vInfo);
            }
          }
          else{
            System.out.println("Il grafo non contiene la categoria: "+vertex.getName());
          }
      }
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

  public static JsonObject getPageInfoById(String pageids){
    JsonObject toReturn = new JsonObject();
    try{

      String query = "https://en.wikipedia.org/w/api.php?action=query&prop=info&pageids="+pageids+"&format=json";
      JsonObject response = CrawlerWikipediaCategory.getJsonResponse(query);
      toReturn = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(pageids).getAsJsonObject();

    }catch (IOException e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    return toReturn;
  }

}

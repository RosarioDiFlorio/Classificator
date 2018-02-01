package eu.innovation.engineering.graph.main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.innovation.engineering.api.WikipediaAPI;
import eu.innovation.engineering.dataset.main.SpringMainLauncher;
import eu.innovation.engineering.dataset.utility.DatasetUtilities;
import eu.innovation.engineering.graph.utility.PathInfo;
import eu.innovation.engineering.graph.utility.Word2Vec;
import eu.innovation.engineering.persistence.EdgeResult;
import eu.innovation.engineering.persistence.SQLiteWikipediaGraph;
import eu.innovationengineering.solrclient.auth.collection.queue.UpdatablePriorityQueue;

public class AnalyzerGraphWikipedia extends SpringMainLauncher {

  private static HashMap<String,Set<String>> mappingTaxonomyWikipedia = null;
  private static Word2Vec word2Vec;
  private static Map<String,EdgeResult> graph;

  private static SQLiteWikipediaGraph dbGraph;
  
  
  /**
   * EXAMPLE AND TEST MAIN
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws Exception {
    mainWithSpring(
        context -> {
          dbGraph = context.getBean(SQLiteWikipediaGraph.class);
          dbGraph.setAutoCommit(false);
          System.out.println(getDocumentLabelsTaxonomy("9912937", true));
          dbGraph.setAutoCommit(true);
        },
        args,
        "classpath:properties-config.xml", "classpath:db-config.xml");
  }

  public static List<String> getDocumentLabelsTaxonomy(String idDocument,boolean withDijstra) throws IOException{
    if(mappingTaxonomyWikipedia == null){
      mappingTaxonomyWikipedia = new HashMap<String, Set<String>>();
      Map<String, List<List<String>>> taxonomyCsv = DatasetUtilities.readTaxomyCSV("wheesbee", false);
      
      for(String wikiCat: taxonomyCsv.keySet()){
        String keyToSave = wikiCat.replace("Category:", "");
        Set<String> toAdd = new HashSet<String>();
        for( List<String> list : taxonomyCsv.get(wikiCat)){
          toAdd.add(list.get(list.size()-1));
        }
        if(!mappingTaxonomyWikipedia.containsKey(keyToSave))
          mappingTaxonomyWikipedia.put(keyToSave, toAdd);
        else
          mappingTaxonomyWikipedia.get(keyToSave).addAll(toAdd);
      }
    }
    List<String> toReturn = new ArrayList<String>();
    List<String> wikipediaLabels = new ArrayList<String>();
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
        pathList = seachBFSMarkedNodes(dbGraph, idDocument, 2);


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
      dbGraph.setAutoCommit(false);
      graph = dbGraph.getGraph("parents");
      dbGraph.setAutoCommit(true);
    }
    //insieme di nodi marcati da ritornare.
    Set<PathInfo> nearestMarkedVertex = new HashSet<PathInfo>();
    Set<String> markedNodes = dbGraph.getMarkedNodes();
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
    UpdatablePriorityQueue<PathInfo> q = new UpdatablePriorityQueue<PathInfo>();
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
   * Return the categories of a Wikipedia's page.
   * Take as input the id of a Wikipedia's document.
   * @param idDocument
   * @return
   * @throws IOException
   */
  public static Set<String> getParentCategoriesByIdPage(String idDocument) throws IOException{
    Set<String> toReturn  = new HashSet<String>();
    JsonArray categoriesParent = null;
    String parentsURL = "https://en.wikipedia.org/w/api.php?action=query&pageids="+idDocument+"&prop=categories&clshow=!hidden&cllimit=500&indexpageids&format=json";
    JsonObject responseParent = WikipediaAPI.getJsonResponse(parentsURL);
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

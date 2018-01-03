package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AnalyzerWikipediaGraph {


  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    CrawlerResult crawlerResults = mapper.readValue(new File(CrawlerResult.class.getSimpleName()), new TypeReference<CrawlerResult>() {});


    //"A-Class_Akwa_Ibom_articles", "A-Class_Andhra_Pradesh_articles_of_Mid-importance", "A-Class_Alberta_articles",
    Set<String> toMark = new HashSet<String>();
    toMark.add("A-Class_Akwa_Ibom_articles");
    toMark.add("A-Class_Andhra_Pradesh_articles_of_Mid-importance");
    toMark.add("1895_in_Egypt");


    String vertexStart = "Years_of_the_19th_century_in_Egypt";

    Map<String, AdjacencyListRow> markedAdjacencyList = markNodes(crawlerResults.getAdjacencyList(), toMark);
    Set<String> result = searchNearestMarkedVertex(markedAdjacencyList, vertexStart, 1);
    
    System.out.println(result);


  }



  public static Set<String> searchNearestMarkedVertex(Map<String,AdjacencyListRow> adjacencyList,String vertexStart,int numberOfMarkedVertex){
    //insieme di nodi marcati da ritornare.
    Set<String> nearestMarkedVertex = new HashSet<String>();
    if(adjacencyList.containsKey(vertexStart)){
      //contatore del numero di nodi marcati trovati.
      int countMarked = 0;
      //controllo se il nodo di partenza è una categoria marcata.
      if(adjacencyList.get(vertexStart).isTaxonomyCategory()){
        nearestMarkedVertex.add(vertexStart);
        countMarked++;
        if(countMarked >= numberOfMarkedVertex)
          return nearestMarkedVertex;
      }
      //lista dei nodi già visitati
      Set<String> visitedVertex = new HashSet<String>();
      //aggiungo il nodo di partenza alla lista dei nodi già visitati.
      visitedVertex.add(vertexStart);
      //coda dei nodi da visitare
      PriorityQueue<String> vertexToVisit = new PriorityQueue<String>();
      //aggiungo tutti i nodi linkati dal nodo di partenza ai nodi da visitare.
      vertexToVisit.addAll(adjacencyList.get(vertexStart).getLinkedVertex());
      //finchè i nodi da visitare non sono terminati.
      while(!vertexToVisit.isEmpty()){
        //prendo il primo elemento della coda.
        String vertex = vertexToVisit.poll();
        //aggiungo il nodo alla lista dei vertici già visitati.
        visitedVertex.add(vertex);
        if(adjacencyList.containsKey(vertex)){
          //se il nodo corrente è una categoria marcata l'aggiungo alla lista da ritornare.
          if(adjacencyList.get(vertex).isTaxonomyCategory()){
            nearestMarkedVertex.add(vertex);
            countMarked++;
            if(countMarked >= numberOfMarkedVertex)
              return nearestMarkedVertex;
          }
          //aggiungo i prossimi nodi da visitare
          for(String v : adjacencyList.get(vertex).getLinkedVertex()){
            if(!visitedVertex.contains(v) && !vertexToVisit.contains(v))
              vertexToVisit.add(v);
          }
        }
      }
    }
    //ritorno la lista di nodi marcati.
    return nearestMarkedVertex;
  }

  public static Map<String,AdjacencyListRow> markNodes(Map<String,AdjacencyListRow> adjacencyList,Set<String> toMark){
    for(String vertex: toMark){
      if(adjacencyList.containsKey(vertex)){
        AdjacencyListRow rowToUpdate = adjacencyList.get(vertex);
        rowToUpdate.setTaxonomyCategory(true);
        adjacencyList.replace(vertex, rowToUpdate);
      }
    }
    return adjacencyList;
  }
}

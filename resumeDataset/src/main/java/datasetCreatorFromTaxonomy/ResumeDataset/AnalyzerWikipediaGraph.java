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

    Map<String, AdjacencyListRow> adjacencyList = markNodes(crawlerResults.getAdjacencyList(), toMark);
    Set<String> result = searchNearestMarkedVertex(adjacencyList, vertexStart, 1);
    
    System.out.println(result);


  }



  public static Set<String> searchNearestMarkedVertex(Map<String,AdjacencyListRow> adjacencyList,String vertexStart,int numberOfMarkedVertex){
    Set<String> nearestMarkedVertex = new HashSet<String>();
    if(adjacencyList.containsKey(vertexStart)){
      int countMarked = 0;
      if(adjacencyList.get(vertexStart).isTaxonomyCategory()){
        nearestMarkedVertex.add(vertexStart);
        countMarked++;
        if(countMarked >= numberOfMarkedVertex)
          return nearestMarkedVertex;
      }
      Set<String> visitedVertex = new HashSet<String>();
      visitedVertex.add(vertexStart);
      PriorityQueue<String> vertexToVisit = new PriorityQueue<String>();
      vertexToVisit.addAll(adjacencyList.get(vertexStart).getLinkedVertex());

      while(!vertexToVisit.isEmpty()){
        String vertex = vertexToVisit.poll();
        visitedVertex.add(vertex);
        if(adjacencyList.containsKey(vertex)){
          if(adjacencyList.get(vertex).isTaxonomyCategory()){
            nearestMarkedVertex.add(vertex);
            countMarked++;
            if(countMarked >= numberOfMarkedVertex)
              return nearestMarkedVertex;
          }
          for(String v : adjacencyList.get(vertex).getLinkedVertex()){
            if(!visitedVertex.contains(v) && !vertexToVisit.contains(v))
              vertexToVisit.add(v);
          }
        }
      }
    }
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

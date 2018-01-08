package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.wikipedia.WikipediaMiner;

public class AnalyzerWikipediaGraph {


  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException{
    Set<String> toMark = new HashSet<String>(getNodeToMark("wheesbee_taxonomy.csv"));
    ObjectMapper mapper = new ObjectMapper();
    CrawlerResult crawlerResults = mapper.readValue(new File(CrawlerResult.class.getSimpleName()), new TypeReference<CrawlerResult>() {});
    Map<String, AdjacencyListRow> markedAdjacencyList = markNodes(crawlerResults.getAdjacencyList(), toMark);
    Set<String> totalResult = new HashSet<String>();
    //11442
    Set<String> belongCategory = WikipediaMiner.getBelongCategories("11442");
    for(String category : belongCategory){
      String vertexStart = WikipediaMiner.getPageInfoById(category).get("title").getAsString().replace("Category:", "");
      System.out.println("Vertice Di Partenza -> "+vertexStart);
 
      Set<String> result = searchNearestMarkedVertex(markedAdjacencyList, vertexStart, 2);
      System.out.println(result);
      System.out.println("-------------------");
      totalResult.addAll(result);
    }
    System.out.println(totalResult);


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
      LinkedList<String> vertexToVisit = new LinkedList<String>();
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
        }else if(adjacencyList.containsKey(vertex.replace("_", " "))){
          vertex = vertex.replace("_", " ");
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
    Set<String> toTest = new HashSet<String>();
    for(String vertex: toMark){
      if(adjacencyList.containsKey(vertex)){
        AdjacencyListRow rowToUpdate = adjacencyList.get(vertex);
        rowToUpdate.setTaxonomyCategory(true);
        adjacencyList.replace(vertex, rowToUpdate);
        toTest.add(vertex);
      }else {
        String newSearch = vertex.replace("_", " ");
        if(adjacencyList.containsKey(newSearch)){
          AdjacencyListRow rowToUpdate = adjacencyList.get(newSearch);
          rowToUpdate.setTaxonomyCategory(true);
          adjacencyList.replace(newSearch, rowToUpdate);
          toTest.add(newSearch);
        }
      }     
    }
    return adjacencyList; 
  }

  public static Set<String> getNodeToMark(String fileWhereRead) throws IOException{
    return readCSV(fileWhereRead, false).keySet();
  }


  private static Map<String,List<String>> readCSV(String csvFile,boolean labeled) throws IOException{

    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<String>> dataMap = new HashMap<String, List<String>>();

    BufferedReader br = new BufferedReader(new FileReader(csvFile));
    if(labeled)
      line = br.readLine();

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
}

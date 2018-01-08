package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AnalyzerWikipediaGraph {


  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException{
    String testDocument = "45712";
    System.out.println(getDocumentLabels(testDocument,3));
  }
  
 
  public static List<String> getDocumentLabels(String idDocument,int limitLabels) throws IOException{

    Set<String> documentCategories = getParentCategoriesByIdPage(idDocument);
    Set<PathInfo> results = new HashSet<PathInfo>();
    
    //leggo la matrice di adiacenza.
    HashMap<String, AdjacencyListRow> adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipedia");
    
    
    for(String category: documentCategories){
      results.addAll(searchNearestMarkedVertex(adjacencyList, category, 3));
    }
    
    List<PathInfo> orderedResults = new ArrayList<PathInfo>(results);
    Collections.sort(orderedResults,Collections.reverseOrder());
    return orderedResults.subList(0,limitLabels).stream().map(e->e.getName()).collect(Collectors.toList());
  }
  
  /**
   * This method is used to do request to obtain parent category
   * @param categories. Category list, used to build request with more category. For any category is returned a list of parent category
   * @return HashMap<String, HashSet<String>>, keys are names of initial categories. HashSet are parent category for any initial category
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
              String title = responseParent.getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();
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

  public static Set<PathInfo> searchNearestMarkedVertex(Map<String,AdjacencyListRow> adjacencyList,String vertexStart,int numberOfMarkedVertex){
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

      ///!!!!!!!!!!!!!!!!! JAVA 8 FUNCTION (conversione di Set<String> in un Set<PathInfo>)
      //trasformo il set di stringhe linkate dal nodo in un set di oggetti PathInfo.
      Set<PathInfo> linkedVertex = adjacencyList.get(vertexStart).getLinkedVertex().stream().map(vertexName-> new PathInfo(vertexName, vertexStartInfo.getValue()+1)).collect(Collectors.toSet());

      //aggiungo tutti i nodi linkati dal nodo di partenza ai nodi da visitare.
      vertexToVisit.addAll(linkedVertex);
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
            if(!visitedVertex.contains(vInfo) && !vertexToVisit.contains(vInfo))
              vertexToVisit.add(vInfo);
          }
        }else if(adjacencyList.containsKey(vertex.getName().replace("_", " "))){
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
      }
    }
    //ritorno la lista di nodi marcati.
    return nearestMarkedVertex;
  }

}

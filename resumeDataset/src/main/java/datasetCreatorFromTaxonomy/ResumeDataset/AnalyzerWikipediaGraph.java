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

public class AnalyzerWikipediaGraph {

  private static  HashMap<String, AdjacencyListRow> adjacencyList = null;
  private static HashMap<String,Set<String>> mappingTaxonomyWikipedia = null;
  private static Map<String,float[]> vectorsWikipediaVertex = null;



  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException{ 
    adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipedia");
    long start = System.currentTimeMillis();
    Set<String> toVectorize = new HashSet<String>(adjacencyList.keySet());

    for(String key: adjacencyList.keySet()){
      toVectorize.addAll(adjacencyList.get(key).getLinkedVertex());  
    }


    getVectorsWikipediaGraph(toVectorize,"vectorsWikipediaVertex");
    //loadVectorsWikipediaGraph("vectorsWikipediaVertex");
    System.out.println(System.currentTimeMillis() - start);
  }

  public static Map<String,float[]> loadVectorsWikipediaGraph(String pathFile) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(new File(pathFile), new TypeReference<Map<String,float[]>>() {});
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
   */
  public static Map<String,float[]> getVectorsWikipediaGraph(Set<String> vertexWikipedia,String pathFile) throws IOException{
    //carico le stopword dal file specificato.
    StopWordEnglish stopWords = new StopWordEnglish("stopwords_en.txt");
    //variabile che specifica ogni quanti elementi deve salvare il tutto. viene incrementato ad ogni salvataggio per evitare che il programma vada in idle scrivendo tutto il tempo sul disco.
    int cutoffSaving = 10000;

    if(vectorsWikipediaVertex == null){
      ObjectMapper mapper = new ObjectMapper();
      List<List<String>> toVectorize = new ArrayList<>();

      if(!new File(pathFile).exists()){ //se il file non esiste istanzio una mappa ex novo.
        vectorsWikipediaVertex = new HashMap<>();      
      }else{    //altrimenti leggo la mappa dal file specificato e rimuovo dall'insieme di vertici da vettorizzare quelli già presenti nella mappa appena caricata.
        vectorsWikipediaVertex =  mapper.readValue(new File(pathFile), new TypeReference<Map<String,float[]>>() {});
        vertexWikipedia.removeAll(vectorsWikipediaVertex.keySet());
        cutoffSaving = (cutoffSaving*3)/2;
      }
      //converto l'insieme di elementi da vettorizzare in una lista in modo da poterci accedere con l'indice.
      List<String> vertexList = vertexWikipedia.stream().collect(Collectors.toList());
      int offset = 0;         //variabile che mi tiene traccia dell'indice corrente.
      for(int i = 0; i<vertexList.size();i++){
        //pulisco i nomi dagli underscore e dalle stop word
        String vertexName = vertexList.get(i).replace("_", " ");
        List<String> cleanVertexName = Arrays.asList(vertexName.split(" ")).stream().filter(el->!stopWords.isStopWord(el)).map(el->el.toLowerCase()).collect(Collectors.toList());
        toVectorize.add(cleanVertexName);
        //ogni tot di vertici eseguo la query al servizio Word2Vec
        if((i % 200 == 0 || i == vertexWikipedia.size()-1) && (i != 0 || vertexList.size() == 1)){
          float[][] vectorizedNames = Word2Vec.returnVectorsFromTextList(toVectorize);
          int count = 0;
          //nel caso vi è un unico elemento da vettorizzare.
          if(i == 0)
            vectorsWikipediaVertex.put(vertexList.get(i).replace(" ", "_"), vectorizedNames[count]);
          //nel caso vi sia più un solo elemento da vettorizzare
          //
          for(int j = (i-offset);j<i;j++){
            vectorsWikipediaVertex.put(vertexList.get(j).replace(" ", "_"), vectorizedNames[count]);
            count++;
          }

          offset =0;
          toVectorize = new ArrayList<>();
          //salvo ogni "cutoffSaving" di elementi la mappa in formato json nel path specificato(pathFile)
          if((i%cutoffSaving == 0 || i == vertexWikipedia.size()-1)){           
            cutoffSaving = (cutoffSaving*3)/2;
            System.out.println("cutoffSaving ->"+cutoffSaving);
            System.out.println("vertexDone->"+vectorsWikipediaVertex.size());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathFile), vectorsWikipediaVertex);
          }
        }
        offset ++;
      }
    }
    return vectorsWikipediaVertex;
  }



  public static List<String> getDocumentLabelsTaxonomy(String idDocument) throws IOException{
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
    List<String> wikipediaLabels = getDocumentLabels(idDocument);
    for(String label: wikipediaLabels){
      if(mappingTaxonomyWikipedia.containsKey(label))
        toReturn.addAll(mappingTaxonomyWikipedia.get(label));
    }
    return toReturn;
  }


  public static void getPath(PathInfo p, StringBuilder builder){
    if (p != null) {
      builder.insert(0, "/" + p.getName());
      getPath(p.getParent(), builder);      
    }
  }



  public static List<String> getDocumentLabels(String idDocument) throws IOException{
    Set<String> documentCategories = getParentCategoriesByIdPage(idDocument);
    Set<PathInfo> results = new HashSet<PathInfo>();
    if(adjacencyList == null)    
      adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipedia");
    for(String category: documentCategories){
      results.addAll(searchNearestMarkedVertexBFS(adjacencyList, category, 3));
    }
    List<PathInfo> orderedResults = new ArrayList<PathInfo>(results);
    Collections.sort(orderedResults,Collections.reverseOrder());
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


  public static void searchNearestMarkedVertexDjistra(Map<String,AdjacencyListRowVertex> adjacencyList,String vertexStartName){

  }



  /**
   * Search the nearest n marked vertex starting to the vertex passed in input to this function.
   * @param adjacencyList
   * @param vertexStart
   * @param numberOfMarkedVertex
   * @return
   */
  public static Set<PathInfo> searchNearestMarkedVertexBFS(Map<String,AdjacencyListRow> adjacencyList,String vertexStart,int numberOfMarkedVertex){
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


}

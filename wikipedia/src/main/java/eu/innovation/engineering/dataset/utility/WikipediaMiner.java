package eu.innovation.engineering.dataset.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.innovation.engineering.graph.utility.PathInfo;
import eu.innovation.engineering.persistence.EdgeResult;
import eu.innovationengineering.solrclient.auth.collection.queue.UpdatablePriorityQueue;


/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class WikipediaMiner{

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(WikipediaMiner.class);









  /**
   * @param queryKey
   * @param typePages
   * @param nameSpace
   * @return
   * @throws IOException
   */
  public static Set<String> getIdsMemberByType(String queryKey, String typePages,int nameSpace,int limitDocument) throws IOException{
    Set<String> toReturn = new HashSet<>();
    if(limitDocument > 500 || limitDocument <= 0)
      limitDocument = 500;


    String queryKeyType = "";
    if(isNumeric(queryKey)){
      queryKeyType = "&cmpageid=";
    }else{
      queryKeyType = "&cmtitle=";
    }
    String targetURL = "https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype="+typePages+queryKeyType+queryKey+"&cmnamespace="+nameSpace+"&cmprop=ids&cmlimit="+limitDocument+"&format=json";
    JsonObject response = getJsonResponse(targetURL);
    JsonArray results = new JsonArray();
    results = response.get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();

    if(results.size()>0){
      for(JsonElement jel: results){      
        toReturn.add(jel.getAsJsonObject().get("pageid").getAsString());
      }
    }
    return toReturn;
  }


  /**
   * Extract info from a certain page.
   * @param pageids
   * @return
   * @throws IOException
   */
  public static JsonObject getPageInfoById(String pageids){
    JsonObject toReturn = new JsonObject();
    try{

      String query = "https://en.wikipedia.org/w/api.php?action=query&prop=info&pageids="+pageids+"&format=json";
      JsonObject response = getJsonResponse(query);
      toReturn = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(pageids).getAsJsonObject();

    }catch (IOException e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    return toReturn;
  }



  /**
   * Return a list of category read from a file.
   * @param pathFile
   * @return
   * @throws IOException
   */
  public static Set<String> getCategoryList(String pathFile) throws IOException{
    FileReader reader = new FileReader(pathFile);
    BufferedReader br = new BufferedReader(reader);
    Set<String> categoryList = new HashSet<String>();
    String line = br.readLine();
    while(line!=null){
      categoryList.add(line);
      line= br.readLine();
    }
    return categoryList;
  }



  /**
   * 
   * @param id
   * @return
   * @throws IOException 
   */
  public static Set<String> getBelongCategories(String queryKey) throws IOException{
    Set<String> idList = new HashSet<String>();

    String queryKeyType = "";
    if(isNumeric(queryKey)){
      queryKeyType = "&pageids=";
    }else{
      queryKeyType = "&titles=";
    }
    //https://en.wikipedia.org/w/api.php?action=query&prop=categories&clshow=!hidden&cldir=ascending&titles=Category:Science
    String query = "https://en.wikipedia.org/w/api.php?action=query&prop=categories&redirects&indexpageids=&clshow=!hidden&cldir=ascending"+queryKeyType+queryKey+"&format=json";
    JsonObject response = getJsonResponse(query);
    String id = response.get("query").getAsJsonObject().get("pageids").getAsJsonArray().get(0).getAsString();
    JsonArray category = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray();

    for(JsonElement obj : category){
      JsonObject newObj = obj.getAsJsonObject();
      String nameCategory = newObj.get("title").getAsString(); 
      idList.add(getIdPage(nameCategory));
    }
    return idList;
  }



  /**
   * @param queryKey
   * @return
   * @throws IOException
   */
  public static String getIdPage(String queryKey) throws IOException{
    String id = "";

    String queryKeyType = "";
    if(isNumeric(queryKey)){
      queryKeyType = "&pageids=";
    }else{
      queryKeyType = "&titles=";
    }
    String targetURL="https://en.wikipedia.org/w/api.php?action=query&indexpageids="+queryKeyType+queryKey.replace(" ", "_")+"&format=json";
    JsonObject response = getJsonResponse(targetURL);
    id = response.get("query").getAsJsonObject().get("pageids").getAsJsonArray().get(0).getAsString();  
    return id;
  }


  /**
   *Extract the content (intro) from a set of pages.
   * @param idPages
   * @return
   * @throws IOException
   */
  public static Map<String, DocumentInfo> getContentPages(Set<String> idPages,int limitDocs) throws IOException{

    Map<String,DocumentInfo> contentPagesMap = new HashMap<>();
    String targetURL = "";
    JsonObject response = new JsonObject();

    /*
     * exlimit
     * How many extracts to return. (Multiple extracts can only be returned if exintro is set to true.)
     * 
     * No more than 20 (20 for bots) allowed.
     * Type: integer or max
     * Default: 20
     */
    int exlimit = 20;
    if(limitDocs< exlimit)
      exlimit = limitDocs;
    LinkedList<String> listId = new LinkedList<>(idPages);
    int countLimit = 0;
    int countDocument = 0;
    String ids = "";
    while(!listId.isEmpty()){
      countLimit++;
      ids += listId.poll()+"|";



      if(countLimit>= exlimit || listId.isEmpty()){
        ids = ids.replaceAll("\\|$", "");
        targetURL = " https://en.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=&exintro=&exlimit="+exlimit+"&pageids="+ids+"&format=json";
        response = getJsonResponse(targetURL); 

        for(String id: ids.split("\\|")){
          String title = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();
          String content = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("extract").getAsString();
          DocumentInfo docInfo = new DocumentInfo();
          docInfo.setId(id);
          docInfo.setText(content);
          docInfo.setTitle(title);
          contentPagesMap.put(id, docInfo);
          countDocument++;
          if(countDocument >= limitDocs)
            return contentPagesMap;
        }
        limitDocs -= exlimit;
        if(limitDocs< exlimit)
          exlimit = limitDocs;
        ids ="";
        countLimit = 0;
      }



    }
    //System.out.println("Extracted content from -> "+contentPagesMap.size()+" documents");
    return contentPagesMap;
  }





  /**
   * Execute a single http request to wikipedia and return the response in json format.
   * @param targetURL
   * @return
   * @throws IOException
   */
  private static  JsonObject getJsonResponse(String targetURL) throws IOException{
    URL url = new URL(targetURL);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setDoOutput(true);
    con.setRequestMethod("GET");
    JsonObject jOb = new JsonObject();
    try{
      Scanner in = new Scanner(new InputStreamReader(con.getInputStream()));  
      JsonParser parser = new JsonParser(); 
      jOb = parser.parse(in.nextLine()).getAsJsonObject();
    }
    catch(ConnectException e){
      System.out.println("Connection timed out: recall method ");
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      jOb = getJsonResponse(targetURL);
    }
    return jOb;
  }

  public static Map<String,DocumentInfo> getContentFromCategoryPages(String category,Map<String,EdgeResult> graph,int limitDocs) throws IOException{

    JsonObject response = new JsonObject();
    Map<String,DocumentInfo> toReturn = new HashMap<>();  
    //prendo gli id delle pagine di questa categoria.

    PathInfo  startVertex = new PathInfo(category, 0);
    UpdatablePriorityQueue<PathInfo> q = new UpdatablePriorityQueue<>();
    Set<PathInfo> visitedCategory = new HashSet<PathInfo>();
    q.add(startVertex);
    while(!q.isEmpty()){
      PathInfo currentVertex = q.poll();
      String name = currentVertex.getName();
      Set<String> idsPages  = getIdsMemberByType(name, "page", 0,limitDocs);    
      idsPages.removeAll(toReturn.keySet());
      toReturn.putAll(getContentPages(idsPages,(limitDocs - toReturn.size())));

      if(toReturn.size() >= limitDocs){
        return toReturn;
      }
      visitedCategory.add(currentVertex);
      djistraUpdate(currentVertex, visitedCategory, q, graph);
    }
    return toReturn;
  }

  public static UpdatablePriorityQueue<PathInfo> djistraUpdate(PathInfo vertexStart,Set<PathInfo> visitedVertex,UpdatablePriorityQueue<PathInfo> q,Map<String,EdgeResult> graph){

    String name = vertexStart.getName().replace("Category:", "");
    if(graph.containsKey(name)){
      EdgeResult currentEdges = graph.get(name);
      List<PathInfo> linkedVertex = currentEdges.getLinkedVertex().stream().map(v->new PathInfo("Category:"+v.getVertexName(),(vertexStart.getValue()+v.getSimilarity()),v.getSimilarity())).collect(Collectors.toList());
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
      linkedVertex.stream().filter(el->!q.contains(el) && !visitedVertex.contains(el)).forEach(q::add);
    }
    return q;
  }


  /**Complete Online Versione using the wikipedia API.
   * @param category
   * @param ids
   * @param recursive
   * @param level
   * @param levelmax
   * @param limitDocs
   * @return
   * @throws IOException
   */
  public static Map<String,DocumentInfo> getContentFromCategoryPages(String category,Set<String> ids,boolean recursive,int level,int levelmax,int limitDocs) throws IOException{
    JsonObject response = new JsonObject();
    Map<String,DocumentInfo> toReturn = new HashMap<>();  
    limitDocs = (limitDocs - ids.size());

    //level 5 is too heavy to compute.
    if(level >= 4)
      return toReturn;
    //prendo gli id delle pagine di questa categoria.
    Set<String> idsPages = getIdsMemberByType(category, "page", 0,limitDocs);   
    idsPages.removeAll(ids);
    toReturn.putAll(getContentPages(idsPages,limitDocs));
    if(toReturn.keySet().size() + ids.size() >= limitDocs){
      Map<String,DocumentInfo> tmpMap = new HashMap<>();
      for(String doc: toReturn.keySet()){
        tmpMap.put(doc, toReturn.get(doc));
        if(tmpMap.size() >= limitDocs || toReturn.isEmpty())
          return tmpMap;
      }   
    }
    if((recursive && level <= levelmax) || (toReturn.keySet().size() < limitDocs && recursive)){
      Set<String> idSubCategories = getIdsMemberByType(category, "subcat", 14,0);
      if(idSubCategories.size()>0){
        for(String idSubCategory: idSubCategories){ 
          toReturn.putAll(getContentFromCategoryPages(idSubCategory,toReturn.keySet(),recursive,level + 1,levelmax,limitDocs));
          if(toReturn.keySet().size()>= limitDocs)
            return toReturn;
        }
      }else
        recursive = false;
    }   
    return toReturn;
  }

  public static boolean isNumeric(String str)  
  {  
    try  
    {  
      double d = Double.parseDouble(str);  
    }  
    catch(NumberFormatException nfe)  
    {  
      return false;  
    }  
    return true;  
  }
}

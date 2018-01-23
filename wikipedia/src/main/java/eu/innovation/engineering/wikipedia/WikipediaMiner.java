package eu.innovation.engineering.wikipedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.innovationengineering.solrclient.auth.collection.queue.UpdatablePriorityQueue;
import persistence.EdgeResult;
import persistence.SQLiteWikipediaGraph;
import utility.PathInfo;

/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class WikipediaMiner{
  
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(WikipediaMiner.class);
  
  
  
  /**
   * @param pathDataset
   * @param datasetMap
   * @param alreadyWritten
   * @throws FileNotFoundException
   */
  public static void writeDocumentMap(String pathDataset,Map<String,Set<DocumentInfo>> datasetMap, Set<String> alreadyWritten) throws FileNotFoundException{   
    for(String wikiCat: datasetMap.keySet()){
      System.out.println("Wikipedia Category -> "+wikiCat+" documents -> "+datasetMap.get(wikiCat).size());
      for(DocumentInfo doc: datasetMap.get(wikiCat)){
        if(!alreadyWritten.contains(doc.getId())){
          alreadyWritten.add(doc.getId());          
          PrintWriter p = new PrintWriter(new File(pathDataset+"/"+wikiCat.replace("Category:", "")+"/"+doc.getId()));
          p.println(doc.getTitle()+"\n"+doc.getText());
          p.flush();
          p.close();
        }             
      }
    }  
  }


  /**
   * @param nameFolders
   * @param pathFolder
   * @return
   */
  public static String buildStructureFolder(Set<String> nameFolders,String pathFolder){
    boolean success = new File(pathFolder).mkdir();
    for(String keyMap: nameFolders){
      success = new File(pathFolder+"/"+keyMap.replace("Category:", "")).mkdir();
    }
    return pathFolder;
  }



  /**
   * @param pathDataset
   * @param categories
   * @param blackList
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static Map<String,Set<DocumentInfo>> buildDatasetOnline(Set<String> categories,int maxLevel,boolean recursive,int limitDocs) throws IOException, InterruptedException, ExecutionException{


    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<>();
    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, maxLevel,recursive,limitDocs);
      datasetTasks.add(task);
    }
    List<Future<Map<String, Set<DocumentInfo>>>> result = pool.invokeAll(datasetTasks);
    Map<String,Set<DocumentInfo>> datasetMap = new HashMap<>();
    for(Future<Map<String, Set<DocumentInfo>>> future : result){
      datasetMap.putAll(future.get());
    }
    return datasetMap;

  }

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
    //System.out.println(targetURL);
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
        }


        countDocument+=countLimit;      
        if(countDocument >= limitDocs)
          return contentPagesMap;

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
    final String USER_AGENT = "Mozilla/5.0";
    URL url = new URL(targetURL);
    //System.out.println(url);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setDoOutput(true);
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", USER_AGENT);
    Scanner in = new Scanner(
        new InputStreamReader(con.getInputStream()));  
    JsonParser parser = new JsonParser(); 
    JsonObject jOb = parser.parse(in.nextLine()).getAsJsonObject();
    return jOb;
  }

  public static Map<String,DocumentInfo> getContentFromCategoryPages(String category,SQLiteWikipediaGraph graph,int limitDocs) throws IOException{
    JsonObject response = new JsonObject();
    Map<String,DocumentInfo> toReturn = new HashMap<>();  
    //prendo gli id delle pagine di questa categoria.
    Set<String> idsPages = getIdsMemberByType(category, "page", 0,limitDocs);   
    toReturn.putAll(getContentPages(idsPages,limitDocs));
    limitDocs = (limitDocs - toReturn.size());
    if(toReturn.size() >= limitDocs){
      return toReturn;
    }
    
    PathInfo  startVertex = new PathInfo(category, 0);
    UpdatablePriorityQueue<PathInfo> q = new UpdatablePriorityQueue<>();
    Set<PathInfo> visitedCategory = new HashSet<PathInfo>();
    q.add(startVertex);
    while(!q.isEmpty()){
      PathInfo currentVertex = q.poll();
      String name = "Category:"+currentVertex.getName();
      toReturn.putAll(getContentFromCategoryPages(name,graph,limitDocs));
      limitDocs = limitDocs - toReturn.size();
      if(toReturn.size() >= limitDocs){
        return toReturn;
      }
      visitedCategory.add(currentVertex);
      djistraUpdate(currentVertex, visitedCategory, q, graph);
    }
    return toReturn;
  }

  public static UpdatablePriorityQueue<PathInfo> djistraUpdate(PathInfo vertexStart,Set<PathInfo> visitedVertex,UpdatablePriorityQueue<PathInfo> q,SQLiteWikipediaGraph graph){
    try{
      EdgeResult currentEdges = graph.getEdgeList(vertexStart.getName(), "childs");
      List<PathInfo> linkedVertex = currentEdges.getLinkedVertex().stream().map(v->new PathInfo(v.getVertexName(),(vertexStart.getValue()+v.getSimilarity()),v.getSimilarity())).collect(Collectors.toList());
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
    }catch (SQLException e) {
      e.printStackTrace();
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

  /**
   * Save a dataset into a folders structure.
   * @param contents
   * @param pathWhereSave
   * @throws FileNotFoundException
   */
  private static Set<String> saveContentFolder(Map<String,DocumentInfo> contents, String pathWhereSave) throws FileNotFoundException{
    boolean success = new File(pathWhereSave).mkdir();  
    for(String documentId: contents.keySet()){
      PrintWriter p = new PrintWriter(new File(pathWhereSave+"/"+documentId));
      p.println(contents.get(documentId).getTitle()+"\n"+contents.get(documentId).getText());
      p.flush();
      p.close();
    }
    return contents.keySet();
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

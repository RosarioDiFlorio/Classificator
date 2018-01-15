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
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class WikipediaMiner implements WikiRequest{
  private static final long serialVersionUID = 1L;

  private static final Logger logger = LoggerFactory.getLogger(WikipediaMiner.class);

  /**
   * @param args
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static void main(String args[]) throws IOException, InterruptedException, ExecutionException{

  }


  private static void mergeFolders(String pathSource,String pathTarget) throws IOException{
    File dir = new File(pathSource);
    List<File> toMove = new ArrayList<File>();
    if(dir.isDirectory()){
      File[] subDirs = dir.listFiles();

      for(File f:subDirs){
        if(f.isDirectory()){
          File target = new File(pathTarget);
          for(File doc:f.listFiles()){
            Files.copy(doc.toPath(), Paths.get(pathTarget+doc.getName()), StandardCopyOption.REPLACE_EXISTING);
          }           
        }
      }


    }
  }


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
  public static Map<String,Set<DocumentInfo>> buildDataset(Set<String> categories,int maxLevel,boolean recursive,int limitDocs,int minLenText) throws IOException, InterruptedException, ExecutionException{


    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<>();

    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, maxLevel,recursive,limitDocs,minLenText);
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
  public static Set<String> getIdsMemberByType(String queryKey, String typePages,int nameSpace) throws IOException{
    Set<String> toReturn = new HashSet<>();

    String queryKeyType = "";
    if(isNumeric(queryKey)){
      queryKeyType = "&cmpageid=";
    }else{
      queryKeyType = "&cmtitle=";
    }
    String targetURL = "https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype="+typePages+queryKeyType+queryKey+"&cmnamespace="+nameSpace+"&cmprop=ids&cmlimit=500&format=json";
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
  public static Map<String, DocumentInfo> getContentPages(Set<String> idPages,int minLenghtText,int limitDocs) throws IOException{

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

    LinkedList<String> listId = new LinkedList<>(idPages);
    int countLimit = 0;
    String ids = "";
    while(!listId.isEmpty()){
      countLimit++;
      ids += listId.poll()+"|";

      if(countLimit>= exlimit || listId.isEmpty()){
        ids = ids.replaceAll("\\|$", "");
        int countDocument = 0;
        targetURL = " https://en.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=&exintro=&pageids="+ids+"&format=json";
        response = getJsonResponse(targetURL); 

        for(String id: ids.split("\\|")){
          String title = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();
          String content = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("extract").getAsString();
          //countrollo sulla lunghezza minima del testo.
          if(content.length() >= 200){
            countDocument++;
            DocumentInfo docInfo = new DocumentInfo();
            docInfo.setId(id);
            docInfo.setText(content);
            docInfo.setTitle(title);
            contentPagesMap.put(id, docInfo);
            if(countDocument >= limitDocs)
              return contentPagesMap;
          }
        }

        ids ="";
        countLimit = 1;
      }



    }
    //System.out.println("Extracted content from -> "+contentPagesMap.size()+" documents");
    return contentPagesMap;
  }

  public static List<String> searchWiki(String keySearch) throws IOException{
    String query = "https://en.wikipedia.org/w/api.php?action=query&list=search&redirects&srnamespace=0&srwhat=text&utf8=&srsearch="+keySearch.replace(" ", "_")+"&format=json";
    JsonObject response = getJsonResponse(query);
    List<String> pages= new ArrayList<>();
    JsonArray pagesJson = response.get("query").getAsJsonObject().get("search").getAsJsonArray();
    if(pagesJson.size()!=0){
      for(int i = 0;i<pagesJson.size();i++){
        String titlePage = response.get("query").getAsJsonObject().get("search").getAsJsonArray().get(i).getAsJsonObject().get("title").getAsString();
        if(!titlePage.matches(".*[0-9]{4}.*"))
          pages.add(titlePage);
      }
    }
    return pages;
  }

  public static WikiCategoryMatch getCategory(String queryKey,String toCompare,WikiCategoryMatch toReturn) throws IOException{
    String queryKeyType = "";
    if(queryKey!= null){
      if(queryKey.contains(" "))
        queryKey = URLEncoder.encode(queryKey,"utf-8");
      if(isNumeric(queryKey)){
        queryKeyType = "&pageids=";
      }else{
        queryKeyType = "&titles=";
      }
      String query ="https://en.wikipedia.org/w/api.php?action=query&prop=categories&redirects&indexpageids=&clshow=!hidden&cldir=ascending"+queryKeyType+queryKey+"&format=json";
      JsonObject response = getJsonResponse(query);
      String id = null;
      JsonArray category = null;
      try{
        id = response.get("query").getAsJsonObject().get("pageids").getAsJsonArray().get(0).getAsString();
        category = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray();


        LevenshteinDistance lDis = new LevenshteinDistance();

        String nameCategory = "";
        double min = 200;
        for(int i=0;i<category.size();i++){
          String tmpName = category.get(i).getAsJsonObject().get("title").getAsString();
          String left = tmpName.replace("Category:", "").toLowerCase();
          String right = queryKey.replace("_", " ").toLowerCase();
          String hint = toCompare.replace("_", " ").toLowerCase();
          double distance = 0;
          double distanceRight = lDis.apply(left,right);
          double distanceHint = lDis.apply(left,hint);

          if(distanceRight< distanceHint)
            distance = distanceRight;
          else{
            distance = distanceHint;
            if(distance == 0){
              toReturn.addExact(tmpName,distance);
            }
          }
          if(distance <= min){

            if(!tmpName.matches(".*[0-9]{4}.*")){
              double distanceHintRight = lDis.apply(hint,right);
              if(distance <=5 && distanceHintRight<=7)
                toReturn.addMajor(tmpName,distance);
              else if(distance<=12)
                toReturn.addMinor(tmpName,distance);
              min = distance;
            }
          }

        }
        return toReturn;
      }catch (Exception e) {
        // TODO: handle exception
        System.out.println(queryKey);
        System.out.println(id);
        System.out.println(response);
        return toReturn;
      }
    }

    else
      return toReturn;
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


  public static Map<String,DocumentInfo> getContentFromCategoryPages(String category,Set<String> ids,boolean recursive,int level,int levelmax,int limitDocs,int minLenText) throws IOException{
    JsonObject response = new JsonObject();
    Map<String,DocumentInfo> toReturn = new HashMap<>();  
    limitDocs = (limitDocs - ids.size());
    
    //level 5 is too heavy to compute.
    if(level >= 4)
      return toReturn;
    
    
    
    //prendo gli id delle pagine di questa categoria.
    Set<String> idsPages = getIdsMemberByType(category, "page", 0);   
    idsPages.removeAll(ids);
    toReturn.putAll(getContentPages(idsPages, minLenText,limitDocs));
    

    
    if(toReturn.keySet().size() + ids.size() >= limitDocs){
      Map<String,DocumentInfo> tmpMap = new HashMap<>();
      for(String doc: toReturn.keySet()){
        tmpMap.put(doc, toReturn.get(doc));
        if(tmpMap.size() >= limitDocs || toReturn.isEmpty())
          return tmpMap;
      }   
    }
    
    if((recursive && level <= levelmax) || (toReturn.keySet().size() < limitDocs && recursive)){
      Set<String> idSubCategories = getIdsMemberByType(category, "subcat", 14);
      if(idSubCategories.size()>0){
        for(String idSubCategory: idSubCategories){ 
              toReturn.putAll(getContentFromCategoryPages(idSubCategory,toReturn.keySet(),recursive,level + 1,levelmax,limitDocs,minLenText));
              if(toReturn.keySet().size()>= limitDocs)
                return toReturn;
        }
      }else
        recursive = false;
    }   
    return toReturn;
  }


  /**
   * For a certain category request the id of all its pages.
   * If recursive is setted true, the function is called for each subcategory of the selected category.
   * @param category specify the selected category.
   * @param ids a set of pages ids.
   * @param recursive specify if the fuction have to be recursive.
   * @param level specify the max level of deepen 
   * @return
   * @throws IOException
   */
  public static Set<String> requestIdsPagesOfCategory(String category,Set<String> ids,boolean recursive,int level,int levelmax,int limitDocs) throws IOException { 
    JsonObject response = new JsonObject();
    //level 5 is too heavy to compute.
    if(level >= 4)
      return ids;

    if((recursive && level <= levelmax) || (ids.size() < limitDocs && recursive)){
      Set<String> idSubCategories = getIdsMemberByType(category, "subcat", 14);
      if(idSubCategories.size()>0){
        for(String idSubCategory: idSubCategories){
          ids = requestIdsPagesOfCategory(idSubCategory,ids,recursive,level + 1,levelmax,limitDocs);
        }
      }else
        recursive = false;
    }
    ids.addAll(getIdsMemberByType(category, "page", 0));
    return ids.stream().limit(limitDocs).collect(Collectors.toSet());
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


  @Override
  public DatasetResponse buildDataset(DatasetRequest request) {
    DatasetResponse response = new DatasetResponse();
    String levelPathFolder = "/var/lib/jetty/data/"+request.getCurrentLevel();
    boolean success = new File(levelPathFolder).mkdir();
    Set<String> categories = request.getCategories();
    Set<String> tosave = new HashSet<>();
    for(String c: categories){
      tosave.add(c.replace("Category:", "").toLowerCase());
    }
    try {

      ObjectMapper mapper = new ObjectMapper();
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(levelPathFolder+"/categories_"+request.getName()+".json"), tosave);

      String pathFolder = levelPathFolder+"/dataset_"+request.getName();
      buildStructureFolder(categories, pathFolder);
      Map<String, Set<DocumentInfo>> datasetMap = buildDataset(request.getCategories(),request.getMaxLevel(), request.isRecursive(),1000,50);
      writeDocumentMap(pathFolder, datasetMap, new HashSet<String>());
      response.setStatus(200);
    }
    catch (Exception e) {
      logger.error("An error occurred while generating dataset", e);
      response.setStatus(500);
      response.setMessage(e.getClass().getCanonicalName() + ": " + e.getMessage());
    }

    return response;

  }
}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;




/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class WikipediaMiner {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static ObjectMapper mapper = new ObjectMapper();


  public WikipediaMiner(){
    
  }


  /**
   * @param args
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static void main(String args[]) throws IOException, InterruptedException, ExecutionException{
    Set<String> categories = getCategoryList("categories.txt");
    Set<String> alreadyWritten = new HashSet<String>();
    String pathDataset = "data/dataset";
    buildDataset("data/dataset", categories,alreadyWritten);
  }


  /**
   * @param pathDataset
   * @param datasetMap
   * @param alreadyWritten
   * @throws FileNotFoundException
   */
  private static void writeDocumentMap(String pathDataset,Map<String,Set<DocumentInfo>> datasetMap, Set<String> alreadyWritten) throws FileNotFoundException{   
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
  private static String buildStructureFolder(Set<String> nameFolders,String pathFolder){
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
  public static void buildDataset(String pathDataset,Set<String> categories,Set<String> blackList) throws IOException, InterruptedException, ExecutionException{
    //costruisco la struttura delle cartelle.
    buildStructureFolder(categories, pathDataset);
    
    ForkJoinPool pool = new ForkJoinPool();
    List<DatasetTask> datasetTasks = new ArrayList<>();
    
    for(String cat : categories){
      DatasetTask task = new DatasetTask(cat, 0,false);
      datasetTasks.add(task);
    }
    
    List<Future<Map<String, Set<DocumentInfo>>>> result = pool.invokeAll(datasetTasks);
    Map<String,Set<DocumentInfo>> datasetMap = new HashMap<>();
    for(Future<Map<String, Set<DocumentInfo>>> future : result){
      datasetMap.putAll(future.get());
    }
    writeDocumentMap(pathDataset, datasetMap,blackList);
  }


  /**
   * Execute a query using the title.
   * @param title
   * @return
   * @throws IOException
   */
  public static Set<String> queryWithTitle(String title) throws IOException{
    String targetURL = "https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&titles="+title+"&format=json";
    JsonObject response = getJsonResponse(targetURL);
    Set<String>idPages =  response.get("query").getAsJsonObject().get("pages").getAsJsonObject().entrySet().stream().map(e->e.getKey().toString()).collect(Collectors.toSet());
    return idPages;
  }


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
  public static JsonObject getPageInfoById(String pageids) throws IOException{
    JsonObject toReturn = new JsonObject();
    String query = "https://en.wikipedia.org/w/api.php?action=query&prop=info&pageids="+pageids+"&format=json";
    JsonObject response = getJsonResponse(query);
    toReturn = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(pageids).getAsJsonObject();
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
    String query = "https://en.wikipedia.org/w/api.php?action=query&prop=categories&indexpageids=&clshow=!hidden&cldir=ascending"+queryKeyType+queryKey+"&format=json";
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
  public static Map<String, DocumentInfo> getContentPages(Set<String> idPages) throws IOException{
    System.out.println("Extracting content from -> "+idPages.size()+" documents");
    Map<String,DocumentInfo> contentPagesMap = new HashMap<>();
    String targetURL = "";
    JsonObject response = new JsonObject();
    int count = 0;
    for(String id:idPages){
      targetURL = " https://en.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=&exintro=&pageids="+id+"&format=json";
      response = getJsonResponse(targetURL);       
      String title = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();
      String content = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("extract").getAsString();
      DocumentInfo docInfo = new DocumentInfo();
      docInfo.setId(id);
      docInfo.setText(content);
      docInfo.setTitle(title);
      contentPagesMap.put(id, docInfo);
      count++;
      /*
    if(count % 10 ==0){
      int percentage = (count*100)/idPages.size();
      System.out.println(percentage+"%");
    }*/
    }
    return contentPagesMap;
  }


 


  /**
   * Load the dataset from a json file.
   * @param pathWhereLoad
   * @return
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  private static Map<String, String> loadContentPages(String pathWhereLoad) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    Map<String,String> contents = mapper.readValue(new File(pathWhereLoad), new TypeReference<Map<String,String>>(){});
    return contents;
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
  public static Set<String> requestIdsPagesOfCategory(String category,Set<String> ids,boolean recursive,int level,int levelmax) throws IOException { 
    JsonObject response = new JsonObject();
    //query for subcategories.
    //https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:Foods&cmnamespace=14&cmprop=ids&cmlimit=500&format=json
    if(recursive && level <= levelmax){
      Set<String> idSubCategories = getIdsMemberByType(category, "subcat", 14);
      if(idSubCategories.size()>0){
        for(String idSubCategory: idSubCategories){
          ids = requestIdsPagesOfCategory(idSubCategory,ids,recursive,level + 1,levelmax);
        }
      }
    }
    ids.addAll(getIdsMemberByType(category, "page", 0));
    return ids;
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

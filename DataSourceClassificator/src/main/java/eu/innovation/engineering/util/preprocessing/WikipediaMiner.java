package eu.innovation.engineering.util.preprocessing;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.innovation.engineering.keyword.extractor.util.LanguageDetector;





/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class WikipediaMiner extends RecursiveTask<List<CategoryInfo>> implements Callable<List<CategoryInfo>>  {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final int  levelLimit = 1;
  private static Map<String,CategoryInfo> catTree = new HashMap<>();
  private List<String> idCategories;
  private List<CategoryInfo> categories;
  private String parent;
  private int level;
  private String root;
  private int maxLevel;


  public WikipediaMiner(List<String> idSubCategoriesLeft, int level, int maxLevel, String root){
    this.parent=parent;
    this.idCategories=idSubCategoriesLeft;
    this.level=level;
    this.root = root;
    this.maxLevel=maxLevel;
  }


  @Override
  protected List<CategoryInfo> compute() {
    ArrayList<CategoryInfo> toReturn = new ArrayList<CategoryInfo>();

    if(level>0){
      for(String currentCategory : idCategories){
        try {
          JsonObject wikipediaQuery = getPageInfoById(currentCategory);
          CategoryInfo currentInfo = new CategoryInfo();
          currentInfo.setId(currentCategory);
          currentInfo.setName(wikipediaQuery.get("title").getAsString());
          currentInfo.setLevel(level);
          toReturn.add(currentInfo);

        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    else{

      try {
        CategoryInfo currentInfo = new CategoryInfo();
        if(isNumeric(root)){
          JsonObject wikipediaQuery = getPageInfoById(root);
          currentInfo.setName(wikipediaQuery.get("title").getAsString());
        }
        else{
          currentInfo.setName(root);
        }
        currentInfo.setId(root);
        currentInfo.setLevel(level);
        toReturn.add(currentInfo);
      }
      catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    //CUTOFF
    if(level == maxLevel){

      return toReturn;
    }
    // divido il problema in sottoproblemi
    else{
      String levelKey = "";
      if(root.equals("root"))
        levelKey = "&cmtitle=";
      else{
        levelKey = "&cmpageid=";
      }
      ArrayList<String> idSubCategories = new ArrayList<String>();
      for(String currentCategory : idCategories){
        String subCategoriesURL ="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype=subcat"+levelKey+currentCategory+"&cmnamespace=14&cmprop=ids&cmlimit=500&format=json";

        try{
          JsonObject response = new JsonObject();
          response = getJsonResponse(subCategoriesURL);
          JsonArray subCategories = new JsonArray();
          subCategories = response.get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
          ArrayList<String> childsNode = new ArrayList<String>();
          if(subCategories.size()>0){ 
            for(JsonElement jel: subCategories){       
              String idSubCategory = jel.getAsJsonObject().get("pageid").getAsString();
              childsNode.add(idSubCategory);  

            }
            idSubCategories.addAll(childsNode);
          }
        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } 
      }

      int size = idSubCategories.size();
      List<String> idSubCategoriesLeft = idSubCategories.subList(0, size/2);
      List<String> idSubCategoriesRight = idSubCategories.subList((size/2)+1, size);

      List<CategoryInfo> leftResult = new ArrayList<>();
      List<CategoryInfo> rightResult =  new ArrayList<>();

      WikipediaMiner leftJob = new WikipediaMiner(idSubCategoriesLeft, level+1,maxLevel,root);
      WikipediaMiner rightJob = new WikipediaMiner(idSubCategoriesRight, level+1,maxLevel,root);
      leftJob.fork();

      rightResult =  rightJob.compute();
      leftResult =  leftJob.join();

      toReturn.addAll(leftResult);
      toReturn.addAll(rightResult);

      // se sono il livello 0, prima di tornare l'array, setto a tutte le categorie il parent
      if(level==0){
        HashSet<String> hashSetRoot = new HashSet<String>();
        hashSetRoot.add(root);
        for(CategoryInfo currentCategory : toReturn){
          currentCategory.setParentSet(hashSetRoot);
        }
      }


      return toReturn;
    }


  }

  private static boolean isNumeric(String str)  
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

  public static void main(String args[]) throws IOException, InterruptedException, ExecutionException{

    List<String> listId = new ArrayList<String>();
    listId.add("4892515");
    listId.add("693016");
    createMapCategoriesWikipedia(listId,"mapArt&Entertainment");

  }




  public static void createMapCategoriesWikipedia(List<String> idList, String nameFile) throws IOException, InterruptedException, ExecutionException{

    ForkJoinPool pool = new ForkJoinPool();
    List<CategoryInfo> categoryList = new ArrayList<CategoryInfo>();
    List<WikipediaMiner>minerList = new ArrayList<WikipediaMiner>();
    
    //per ogni id creo un miner
    for(String id:idList){
      ArrayList<String> list = new ArrayList<>();
      list.add(id);
      WikipediaMiner miner = new WikipediaMiner(list,0, 4, id);
      minerList.add(miner);
    }
    
    //lancio i miners
    List<Future<List<CategoryInfo>>> response = pool.invokeAll(minerList);
    
    //aggiungo le categorie ottenute ad una lista di categorie
    for(Future<List<CategoryInfo>> f: response ){
      List<CategoryInfo> currentList = f.get();
      for(CategoryInfo c : currentList){
        categoryList.add(c);
      }
    }

    //creo la mappa basandomi sulla lista delle categorie
    Map<String, CategoryInfo> map = createMapCategory(categoryList);
    JsonPersister.saveObject(nameFile+".json", map);

    for(String key: map.keySet()){
      System.out.println("id:"+key+" name:"+map.get(key).getName()+" parents:"+map.get(key).getParentSet());
    }

  }
  public static Map<String, CategoryInfo> createMapCategory(List<CategoryInfo> categoryList){

    Map<String,CategoryInfo> mapToReturn = new HashMap<String,CategoryInfo>();
    for(CategoryInfo c: categoryList){
      //se contiene già la categoria
      if(mapToReturn.containsKey(c.getId())){
        if(c.getLevel()<mapToReturn.get(c.getId()).getLevel())
          mapToReturn.get(c.getId()).setLevel(c.getLevel());

        Set<String> currentParents = c.getParentSet(); 
        //per ogni parent della categoria corrente
        for(String parent:currentParents){
          //se il parent corrente della categoria corrente non c'è nella categoria che già è presente in mappa
          if(!mapToReturn.get(c.getId()).getParentSet().contains(parent)){
            Set<String> toAdd = mapToReturn.get(c.getId()).getParentSet();
            toAdd.add(parent);
            mapToReturn.get(c.getId()).setParentSet(toAdd);
          }
        }
      }
      else{
        mapToReturn.put(c.getId(), c);
      }

    }

    for(String key : mapToReturn.keySet()){
      CategoryInfo c = mapToReturn.get(key);
      c.setParentSet(c.getParentSet().stream().filter(p -> mapToReturn.get(p).getLevel()!= c.getLevel()).collect(Collectors.toSet()));
    }
    return mapToReturn;

  }

  /**
   * Build a dataset from wikipia in txt format and json format.
   * @param categories A list of wikipedia category.
   * @param pathWhereSave specify the path where save the dataset.
   * @param txtFolder specify the format.
   * @param recursive specify the recursion
   * @throws IOException
   */
  public static void buildDatasets(List<String> categories,String pathWhereSave,boolean txtFolder,boolean recursive) throws IOException{

    for(String category : categories){
      if(txtFolder)
        saveContentFolder(getContentPages(requestIdsPagesOfCategory(category,new HashSet<String>(),recursive,0)), pathWhereSave);
      else
        saveContentPages(getContentPages(requestIdsPagesOfCategory(category,new HashSet<String>(),recursive,0)), pathWhereSave);
    }
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
   *Extract the content (intro) from a set of pages.
   * @param idPages
   * @return
   * @throws IOException
   */
  public static Map<String, String> getContentPages(Set<String> idPages) throws IOException{
    System.out.println("Extracting content from -> "+idPages.size()+" documents");
    Map<String,String> contentPagesMap = new HashMap<>();
    String targetURL = "";
    JsonObject response = new JsonObject();
    int count = 0;
    for(String id:idPages){
      targetURL = " https://en.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=&exintro=&pageids="+id+"&format=json";
      response = getJsonResponse(targetURL);       
      String title = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();
      String content = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("extract").getAsString();
      contentPagesMap.put(id, title+"\n"+content);
      count++;
      if(count % 10 ==0){
        int percentage = (count*100)/idPages.size();
        System.out.println(percentage+"%");
      }
    }
    return contentPagesMap;
  }

  /**
   * Save the dataset in json format.
   * @param contents
   * @param pathWhereSave
   * @throws JsonGenerationException
   * @throws JsonMappingException
   * @throws IOException
   */
  private static void saveContentPages(Map<String,String> contents, String pathWhereSave) throws JsonGenerationException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathWhereSave), contents);
  }

  /**
   * Save a dataset into a folders structure.
   * @param contents
   * @param pathWhereSave
   * @throws FileNotFoundException
   */
  private static void saveContentFolder(Map<String,String> contents, String pathWhereSave) throws FileNotFoundException{
    boolean success = new File(pathWhereSave).mkdir();  
    for(String documentId: contents.keySet()){
      PrintWriter p = new PrintWriter(new File(pathWhereSave+"/"+documentId));
      p.println(contents.get(documentId));
      p.flush();
      p.close();
    }
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
  private static Set<String> requestIdsPagesOfCategory(String category,Set<String> ids,boolean recursive,int level) throws IOException { 
    JsonObject response = new JsonObject();
    //query for subcategories.
    //https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:Foods&cmnamespace=14&cmprop=ids&cmlimit=500&format=json
    if(recursive && level <= levelLimit){
      Set<String> idSubCategories = getIdsMemberByType(category, "subcat", 14);
      if(idSubCategories.size()>0){
        for(String idSubCategory: idSubCategories){
          ids = requestIdsPagesOfCategory(idSubCategory,ids,recursive,level + 1);
        }
      }
    }
    ids.addAll(getIdsMemberByType(category, "page", 0));
    return ids;
  }



  public static Set<String> getIdsMemberByType(String queryKey, String typePages,int nameSpace) throws IOException{
    Set<String> toReturn = new HashSet<>();
    
    String queryKeyType = "";
    if(isNumeric(queryKeyType)){
      queryKeyType = "&cmpageid=";
    }else{
      queryKeyType = "&cmtitle=";
    }
    
    String targetURL = "https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype="+typePages+queryKeyType+queryKey+"&cmnamespace="+nameSpace+"&cmprop=ids&cmlimit=500&format=json";
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
   * Check if a word is valid.
   * @param detector
   * @param word
   * @param languageFilter
   * @return
   */
  private static boolean  isValidWord(LanguageDetector detector, String word,String languageFilter){
    word = word.replaceAll("[0-9]*", "");
    if(word.length()<=2)
      return false;
    return detector.isValidLanguage(word, languageFilter);   
  }

  /**
   * Return a list of category read from a file.
   * @param pathFile
   * @return
   * @throws IOException
   */
  public static List<String> getCategoryList(String pathFile) throws IOException{
    FileReader reader = new FileReader(pathFile);
    BufferedReader br = new BufferedReader(reader);
    ArrayList<String> categoryList = new ArrayList<String>();
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
    if(isNumeric(queryKeyType)){
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
    if(isNumeric(queryKeyType)){
      queryKeyType = "&pageids=";
    }else{
      queryKeyType = "&titles=";
    }
    String targetURL="https://en.wikipedia.org/w/api.php?action=query&indexpageids="+queryKeyType+queryKey+"&format=json";
    JsonObject response = getJsonResponse(targetURL);
    id = response.get("query").getAsJsonObject().get("pageids").getAsJsonArray().get(0).getAsString();  
    return id;
  }


  @Override
  public List<CategoryInfo> call() throws Exception {
    return this.compute();

  }









}

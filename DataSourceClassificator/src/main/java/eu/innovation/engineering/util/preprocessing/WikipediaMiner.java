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
import java.util.Arrays;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import eu.innovation.engineering.config.PathConfigurator;
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

          toReturn.add(currentInfo);

        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    else{
      CategoryInfo currentInfo = new CategoryInfo();
      currentInfo.setId(root);
      currentInfo.setName(root);
      toReturn.add(currentInfo);
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

        try {
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

  public static void main(String args[]) throws IOException, InterruptedException, ExecutionException{

    createMapCategoriesWikipedia();
    //mainTocreateMap();

  }



  public static void createMapCategoriesWikipedia() throws IOException, InterruptedException, ExecutionException{

    ArrayList<String> list = new ArrayList<>();
    list.add("Category:Main_topic_classifications");

    ForkJoinPool pool = new ForkJoinPool();
    WikipediaMiner miner = new WikipediaMiner(list,0, 1, "root");
    List<CategoryInfo> result1 = pool.invoke(miner);
    result1.stream().map(c -> c.getId()).forEach(System.out::println);
    List<CategoryInfo> result2 = new ArrayList<>();
    List<WikipediaMiner> minerTasks = new ArrayList<WikipediaMiner>();


    for(CategoryInfo cat : result1){
      System.out.println(cat.getName());
      if(!cat.getId().equals("root")){

        list = new ArrayList<>();
        list.add(cat.getId());
        miner = new WikipediaMiner(list,0, 1, cat.getId());
        minerTasks.add(miner);
      }  
    }

    List<Future<List<CategoryInfo>>> response = pool.invokeAll(minerTasks);
    List<CategoryInfo> categoryList = new ArrayList<CategoryInfo>();
    for(Future<List<CategoryInfo>> f: response ){
      List<CategoryInfo> currentList = f.get();
      for(CategoryInfo c : currentList){
        categoryList.add(c);
      }
    }
    

    Map<String, CategoryInfo> map = createMapCategory(categoryList);

    
    for(String key: map.keySet()){
      
      System.out.println("id:"+key+" name:"+map.get(key).getName()+" parents:"+map.get(key).getParentSet());
      
    }
    
    
    /*System.out.println(map.size());

    for(String key : map.keySet()){
      if(map.get(key).getParentSet().size()>1){
        System.out.println(map.get(key).getName() +" -> "+map.get(key).getParentSet());
        for(String parent :map.get(key).getParentSet()){
          JsonObject pageInfo = getPageInfoById(parent);
          String parentName = pageInfo.get("title").getAsString();
          System.out.print("     "+parentName);
        }
        System.out.println();
      }
    }*/
   



  }


  public static void mainTocreateMap() throws IOException{

    readerCategoryWikipediaFromFileTXT("categoriesWikiedia.csv");

  }


  public static void readerCategoryWikipediaFromFileTXT(String path) throws IOException{

    FileReader reader = new FileReader(path);
    BufferedReader br = new BufferedReader(reader);

    //leggo la prima riga
    String line = br.readLine();
    //leggo la seconda riga per saltare l'intestazione del csv
    line = br.readLine();
    List<CategoryInfo> categoryList = new ArrayList<CategoryInfo>();
    while(line!=null){
      String[] lineSplitted = line.split(",");
      CategoryInfo category = new CategoryInfo();
      category.setId(lineSplitted[0]);
      category.setName(lineSplitted[1]);
      Set<String> parent = new HashSet<String>();
      parent.add(lineSplitted[2].replace("[","").replace("]",""));
      category.setParentSet(parent);
      categoryList.add(category);
      line = br.readLine();
    }

    System.out.println(categoryList.size());

    Map<String, CategoryInfo> map = createMapCategory(categoryList);

    System.out.println(map.size());

    for(String key : map.keySet()){
      if(map.get(key).getParentSet().size()>1){
        System.out.println(map.get(key).getName() +" -> "+map.get(key).getParentSet());
        for(String parent :map.get(key).getParentSet()){
          JsonObject pageInfo = getPageInfoById(parent);
          String parentName = pageInfo.get("title").getAsString();
          System.out.print("     "+parentName);
        }
        System.out.println();
      }
    }


  }



  public static Map<String, CategoryInfo> createMapCategory(List<CategoryInfo> categoryList){

    Map<String,CategoryInfo> mapToReturn = new HashMap<String,CategoryInfo>();

    for(CategoryInfo c: categoryList){


      //se contiene già la categoria
      if(mapToReturn.containsKey(c.getId())){
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

    return mapToReturn;

  }





  /**
   * Example Main to create datasets from wikipedia and create glossaries.
   * @param args
   * @throws IOException
   */
  public static void main2(String[] args) throws IOException{

    /*
     * CREATION OF THE DATASETS FROM WIKIPEDIA
     */
    String title = "Glossary_of_chemistry_terms";





    String categoryPathFile = PathConfigurator.applicationFileFolder+"wikiCategories.txt";
    List<String> categories = new ArrayList<String>();
    //getCategoryList(categoryPathFile);
    /*
     * nuclear chemistry
Inorganic chemistry
Organic chemistry
Biochemistry
     */
    String pathWhereSave = PathConfigurator.pyFolder+"biochemistry";
    categories.add("Biochemistry");
    buildDatasets(categories, pathWhereSave,true,true);

    /*
     *CREATION OF GLOSSARIES. 
     *
    String category = "travel";
    buildGlossary(title, pathWhereSave);
    buildGlossaries(category, pathWhereSave); 


    String pathFolder ="glossaries/";
    File folder = new File(pathFolder);
    File[] glossaries = folder.listFiles();

    List<String> glossariesToMerge = new ArrayList<>();
    for(int i=0;i<glossaries.length;i++){
      glossariesToMerge.add(pathFolder+glossaries[i].getName());
    }
    saveGlossary(mergeGlossaries(glossariesToMerge), "food.json");
     */


    //glossariesToMerge.add("science.json");
    //glossariesToMerge.add("mathematics.json");
    //saveGlossary(mergeGlossaries(glossariesToMerge), "glossaries.json");
  }

  public static void mainLuigi(String[] args) throws IOException{
    String path ="WikipediaData/Wikipediacategories.json";
    Map<String, CategoryInfo> categoryList = loadJsonWikipediaCategories(path);
    //System.out.println(categoryList);
    Set<String> listId = getCategoryIdByIdSource("6788582");    
    getParentCategoryList(listId,categoryList);
  }





  /**
   * Build a glossary from a single page.
   * @param title
   * @param pathWhereSave
   * @throws IOException
   */
  public static void buildGlossary(String title, String pathWhereSave) throws IOException{
    saveGlossary(getGlossaryTerms(queryWithTitle(title), true), pathWhereSave);
  }

  /**
   * 
   * Build a glossarry from a certain category.
   * @param category
   * @param pathWhereSave
   * @throws JsonGenerationException
   * @throws JsonMappingException
   * @throws IOException
   */
  public  static void buildGlossaries(String category,String pathWhereSave) throws JsonGenerationException, JsonMappingException, IOException{
    saveGlossary(getGlossaryTerms(requestIdsOfCategory(category,new HashSet<String>(), true,0),true), pathWhereSave);
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
        saveContentFolder(getContentPages(requestIdsOfCategory(category,new HashSet<String>(),recursive,0)), pathWhereSave);
      else
        saveContentPages(getContentPages(requestIdsOfCategory(category,new HashSet<String>(),recursive,0)), pathWhereSave);
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
   * Extract the terms from a glossary page.
   * @param set
   * @param singleWords
   * @return
   * @throws IOException
   */
  public static Map<String,Set<String>> getGlossaryTerms(Set<String> set, boolean singleWords) throws IOException{
    Map<String,Set<String>> glossaryMapTerms = new HashMap<>();
    String targetURL = "";
    JsonObject response = new JsonObject();

    for(String id: set){
      targetURL = "https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&pageids="+id+"&format=json";
      response = getJsonResponse(targetURL);

      String title = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();

      String content = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("revisions").getAsJsonArray()
          .get(0).getAsJsonObject().get("*").toString();
      content = content.replace("'", "");
      content.replace("glossary", "");

      List<String> textSplitted = new ArrayList<>();
      if(content.matches("== *[a-zA-Z] *=="))
        textSplitted.addAll(Arrays.asList(content.split("== *[a-zA-Z] *==")));
      else
        textSplitted.add(content);

      if(textSplitted.isEmpty()){
        System.out.println("Page format not supported -> "+title);
        continue;
      }
      glossaryMapTerms.put(title, new HashSet<String>());
      for(int i= 0; i<textSplitted.size();i++){     
        Pattern patt = Pattern.compile("\\[\\[(.*?)\\]\\]");
        String toMatch = textSplitted.get(i);
        Matcher m = patt.matcher(toMatch);
        StringBuffer sb = new StringBuffer();
        int count = 0;
        while (m.find()) {        
          if(!m.group(0).contains("|") && !m.group(0).contains(":") && !m.group(0).contains("-") && !m.group(0).contains("#") && !m.group(0).contains("glossary") ){
            String term = m.group(1).toLowerCase();
            if(singleWords){   
              if(!term.contains(" ") && term.length()>2){
                glossaryMapTerms.get(title).add(term);
              }         
            }else{
              glossaryMapTerms.get(title).add(m.group(1));
            }
            m.appendReplacement(sb, "");
          }
        }
        m.appendTail(sb);        
      }
      if(glossaryMapTerms.get(title).isEmpty() || glossaryMapTerms.get(title).size()<5)
        glossaryMapTerms.remove(title);
      else{
        System.out.println("Pages readed successfully");
        System.out.println(title+" -> "+glossaryMapTerms.get(title).size());
      }   
    }
    return glossaryMapTerms;
  }

  /**
   * Save a glossary in json format.
   * @param glossary
   * @param pathWhereSave
   * @throws JsonGenerationException
   * @throws JsonMappingException
   * @throws IOException
   */
  private static void saveGlossary(Map<String,Set<String>> glossary,String pathWhereSave) throws JsonGenerationException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathWhereSave), glossary);
  }

  /**
   * Load a glossary from json format.
   * @param pathWhereLoad
   * @return
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  private static Map<String,Set<String>> loadGlossary(String pathWhereLoad) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    Map<String,Set<String>> glossary = mapper.readValue(new File(pathWhereLoad), new TypeReference<Map<String,Set<String>>>() {});
    return glossary;
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



  private static void categoriesTree(String category,String parent, int level) throws IOException{

    if(level>levelLimit){
      return;
    }
    System.out.println("level-> "+level);

    JsonObject response = new JsonObject();
    CategoryInfo nodeInfo = new CategoryInfo();
    Set<String> childsNode = new HashSet<>();

    //if root
    if(category.equals("Main_topic_classifications")){
      String subCategoriesURL ="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype=subcat&cmtitle=Category:"+category+"&cmnamespace=14&cmprop=ids&cmlimit=500&format=json";
      response = getJsonResponse(subCategoriesURL);
      JsonArray subCategories = new JsonArray();
      subCategories = response.get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();

      if(subCategories.size()>0){
        for(JsonElement jel: subCategories){   
          String idSubCategory = jel.getAsJsonObject().get("pageid").getAsString();
          childsNode.add(idSubCategory);
        }
        nodeInfo.setChildSet(childsNode);
        nodeInfo.setName("Category:Main_topic_classifications");
        catTree.put("root", nodeInfo);

        for(String idSubCategory:childsNode){
          categoriesTree(idSubCategory, "root", level+1);
        }
      }
    }else{
      JsonObject pageInfo = getPageInfoById(category);
      nodeInfo.setName(pageInfo.get("title").getAsString());
      String subCategoriesURL ="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype=subcat&cmpageid="+category+"&cmnamespace=14&cmprop=ids&cmlimit=500&format=json";
      response = getJsonResponse(subCategoriesURL);
      JsonArray subCategories = new JsonArray();
      subCategories = response.get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
      if(subCategories.size()>0){ 
        for(JsonElement jel: subCategories){       
          String idSubCategory = jel.getAsJsonObject().get("pageid").getAsString();
          childsNode.add(idSubCategory);     
        }
      }
      nodeInfo.setChildSet(childsNode);

      if(catTree.containsKey(category)){
        Set<String> parentSet = catTree.get(category).getParentSet();
        if(!parentSet.contains(parent) && !childsNode.contains(parent)){
          parentSet.add(parent);
        }
        nodeInfo.setParentSet(parentSet);
        catTree.replace(category, nodeInfo);

      }else{
        Set<String> parentSet = new HashSet<>();
        if(!childsNode.contains(parent))
          parentSet.add(parent);
        nodeInfo.setParentSet(parentSet);
        catTree.put(category, nodeInfo);
      }

      if(level ==1)
        parent = category;

      for(String idSubCategory:childsNode){
        categoriesTree(idSubCategory, parent, level+1);
      }

      ObjectMapper mapper = new ObjectMapper();
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File("categoryTree.json"), catTree);

    }

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
  private static Set<String> requestIdsOfCategory(String category,Set<String> ids,boolean recursive,int level) throws IOException { 
    JsonObject response = new JsonObject();
    //query for subcategories.
    //https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:Foods&cmnamespace=14&cmprop=ids&cmlimit=500&format=json
    if(recursive && level <= levelLimit){
      String subCategoriesURL ="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype=subcat&cmtitle=Category:"+category+"&cmnamespace=14&cmprop=ids&cmlimit=500&format=json";
      response = getJsonResponse(subCategoriesURL);
      JsonArray subCategories = new JsonArray();
      subCategories = response.get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();

      if(subCategories.size()>0){
        for(JsonElement jel: subCategories){
          String idSubCategory = jel.getAsJsonObject().get("pageid").getAsString();
          JsonObject infoSubCategory = getPageInfoById(idSubCategory);
          String subCategoryName = infoSubCategory.get("title").getAsString().replace("Category:", "").replace(" ","_");
          ids = requestIdsOfCategory(subCategoryName,ids,recursive,level + 1);
        }
      }
    }
    //System.out.println(ids.size());
    //https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmpageid=36812606&cmnamespace=14&cmprop=ids&cmlimit=500&format=json

    String targetURL = "https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtype=page&cmtitle=Category:"+category+"&cmnamespace=0&cmprop=ids&cmlimit=500&format=json";
    response = getJsonResponse(targetURL);
    JsonArray results = new JsonArray();
    results = response.get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();

    if(results.size()>0){
      for(JsonElement jel: results){
        ids.add(jel.getAsJsonObject().get("pageid").getAsString());
      }
      System.out.println("Requesting Ids "+category +" level -> "+level +" number of pages -> "+results.size());

    }

    return ids;
  }

  /**
   * Extract info from a certain page.
   * @param pageids
   * @return
   * @throws IOException
   */
  private static JsonObject getPageInfoById(String pageids) throws IOException{
    JsonObject toReturn = new JsonObject();
    String query = "https://en.wikipedia.org/w/api.php?action=query&prop=info&pageids="+pageids+"&format=json";
    JsonObject response = getJsonResponse(query);
    toReturn = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(pageids).getAsJsonObject();
    return toReturn;
  }


  /**
   * Merge a set of glossaries in a single glossary.
   * @param pathGlossaries
   * @return
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  public static Map<String,Set<String>> mergeGlossaries(List<String> pathGlossaries) throws JsonParseException, JsonMappingException, IOException{
    Map<String, Set<String>> glossaryMerged = new HashMap<>();
    LanguageDetector detector = new LanguageDetector(); 
    for(String pathGlossary: pathGlossaries){

      Map<String, Set<String>> tmpGloss = loadGlossary(pathGlossary);
      for(String keyTmp: tmpGloss.keySet()){
        tmpGloss.replace(keyTmp, tmpGloss.get(keyTmp).stream().map(s->s= s.toLowerCase()).filter(s -> isValidWord(detector,s, "en") == true).collect(Collectors.toSet()));
      }
      glossaryMerged.putAll(tmpGloss);
    }
    return glossaryMerged;
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
   * Return a parent category list from input categories 
   * @param categoriesToSearch category used to search parent category
   * @return parent category list
   */

  public static Set<String> getParentCategoryList(Set<String> categoriesToSearch, Map<String,CategoryInfo> categoryList){

    Set<String> parentCategoryList = new HashSet<String>();

    for(String category : categoriesToSearch){

      System.out.println("----------------------------------------------------------");
      if(categoryList.get(category)!=null){
        System.out.println(categoryList.get(category).getName()+" "+category);
        Set<String> parentCurrentCategoryList = categoryList.get(category).getParentSet();
        parentCategoryList.addAll(parentCurrentCategoryList);
        for(String superCategory: parentCurrentCategoryList){
          System.out.println("    "+categoryList.get(superCategory).getName()+" "+superCategory);
        }
      }
      else{
        System.out.println(category+" there isn't into json file");
      }
    }


    return parentCategoryList;

  }

  /**
   * 
   * @param id
   * @return
   * @throws IOException 
   */
  public static Set<String> getCategoryIdByIdSource(String id) throws IOException{

    Set<String> idList = new HashSet<String>();
    //https://en.wikipedia.org/w/api.php?action=query&indexpageids=&titles=Category:Collecting&format=json
    String query = "https://en.wikipedia.org/w/api.php?action=query&prop=categories&pageids="+id+"&format=json";
    JsonObject response = getJsonResponse(query);

    JsonArray category = response.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray();

    for(JsonElement obj : category){
      //String nameCategory = obj.getAsString();
      JsonObject newObj = obj.getAsJsonObject();
      String nameCategory = newObj.get("title").getAsString();
      String query2="https://en.wikipedia.org/w/api.php?action=query&indexpageids=&titles="+nameCategory.replace(" ", "%20")+"&format=json";
      response = getJsonResponse(query2);
      idList.add(response.get("query").getAsJsonObject().get("pageids").getAsString());

    }

    return idList;
  }




  /**
   * to read json file and return object readed.
   * @param path file to read
   * @return Map<String,CategoryInfo>
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  public static Map<String,CategoryInfo> loadJsonWikipediaCategories(String path) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();

    //JSON from file to Object
    Map<String,CategoryInfo> obj = mapper.readValue(new File(path),  new TypeReference<Map<String,CategoryInfo>>() {});

    return obj;


  }


  @Override
  public List<CategoryInfo> call() throws Exception {
    return this.compute();

  }









}

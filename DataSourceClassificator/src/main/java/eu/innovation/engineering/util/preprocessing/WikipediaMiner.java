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

import eu.innovation.engineering.keyword.extractor.util.LanguageDetector;




/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class WikipediaMiner {

  private static final int  levelLimit = 0;

  /**
   * Example Main to create datasets from wikipedia and create glossaries.
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException{
    
    /*
     * CREATION OF THE DATASETS FROM WIKIPEDIA
     */
    String title = "Glossary_of_chemistry_terms";


    String pathWhereSave = "art.and.entertainment";


    String categoryPathFile = "categories.txt";
    List<String> categories = getCategoryList(categoryPathFile);
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



}

package eu.innovation.engineering.prepocessing.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Concept;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Sentiment;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.language.detector.impl.CybozuLanguageDetector;
import maui.main.MauiWrapper;




/**
 * This class takes as input a raw json file with raw documents and give in output a 
 * file json with the ids, titles and keywords for each document of the dataset.
 * @author Rosario
 *
 */
public class PaperBuilder {


  //Main che prende in input un file json e crea il corrispondente con le keywords
  public static void main(String args[]) throws IOException{

    PaperBuilder builder = new PaperBuilder();
    builder.buildDataset("datasetJsonSOLR/datasetForTestSOLR.json");

  }
  /**
   * Old variables of deprecated methods.
   */
  private File toParse;
  private org.w3c.dom.Document doc;


  private HashMap<String,ArrayList<Paper>> categoryMap;
  private ArrayList<Paper> listPapers;



  private MauiWrapper wrapper;
  private ObjectMapper mapper;
  private CybozuLanguageDetector languageDetector;
  private KeywordExtractor kExtractor;
  
  private static final String  vocabularyName = "none";
  private static final String modelName = "InnenModel";
  private static final String dataDirectory = "Maui1.2/";



  private static String datasetPath = "datasetJsonSOLR/datasetWithKeywords2.json";

  public PaperBuilder(){
    this.listPapers = new ArrayList<Paper>();
    this.categoryMap = new HashMap<>();
    this.wrapper = new MauiWrapper(dataDirectory, vocabularyName, modelName);
    this.mapper = new ObjectMapper();
    this.kExtractor = new KeywordExtractor();
    this.kExtractor.setStopWordPath("../DataSourceKeywords/"+this.kExtractor.getStopWordPath());
    
    
    this.languageDetector = new CybozuLanguageDetector();

  }


  public ArrayList<Keyword> extractKeywords(List<String> texts) throws LanguageException{
    ArrayList<Keyword> toReturn = new ArrayList<>();
    toReturn = (ArrayList<Keyword>) this.kExtractor.extractKeywordFromTexts(texts);
    return toReturn;
  }

  public ArrayList<Keyword> analyzeTextByMaui(String text) throws Exception{
    ArrayList<Keyword> toReturn = new ArrayList<>();

    toReturn = this.wrapper.extractKeywordsFromText(text, 10);

    return toReturn;
  }


  /**
   * @deprecated
   * @param parentFolder
   * @return
   */
  @Deprecated
  public HashMap<String,ArrayList<Keyword>> parseJsonFolder(String parentFolder){
    //carico tutti i file json
    //String parentFolder = "dataset/json/";
    HashMap<String,ArrayList<Keyword>> toReturn = new HashMap<>();
    File folder = new File(parentFolder);
    File[] listOfFiles = folder.listFiles();

    for(int i = 0; i<listOfFiles.length; i++){
      System.out.println(parentFolder+listOfFiles[i].getName());
      toReturn.putAll(this.parseJsonFile(parentFolder+listOfFiles[i].getName()));
    }


    return toReturn;
  }


  /**
   * @deprecated
   * @param toParse
   * @return
   */
  @Deprecated
  public HashMap<String,ArrayList<Keyword>> parseJsonFile(String toParse){
    HashMap<String,ArrayList<Keyword>> toReturn = new HashMap<>();
    JsonParser parser = new JsonParser();   

    String id ="";
    String description = "";

    try{

      FileReader read = new FileReader(toParse);
      JsonArray docs = parser.parse(read).getAsJsonObject().get("response").getAsJsonObject().getAsJsonArray("docs");


      for(int i = 0; i< docs.size(); i++){
        JsonObject doc = docs.get(i).getAsJsonObject();


        id = doc.get("id").getAsString();
        description = doc.get("dc_description").getAsString();

        toReturn.put(id, null);
        if(!description.equals("null")){
          ArrayList<Keyword> keys = this.analyzeTextByMaui(description); 
          toReturn.put(id, keys);
        }else{
          toReturn.put(id, null);
        }
      }
    }catch (Exception e) {
      // TODO: handle exception
      if(e.getMessage().matches("Text is too short!")){
        System.out.println("Problem for paper: "+id);
      }
    }



    return toReturn;


  }






  /**
   * This take the dataset Json created by solr 
   * analizes the description for each paper 
   * and builds the data structures of the paperbuilder.
   * @param filename
   * @throws IOException 
   * @throws Exception 
   */
  public void buildDataset(String filename) throws IOException{

    this.languageDetector.init();

    try {
      System.out.println("Loading "+filename +" from " + this.getDatasetPath());
      this.listPapers = mapper.readValue(new File(this.getDatasetPath()),  new TypeReference<ArrayList<Paper>>(){} );
      System.out.println("Completed N° documents: "+this.listPapers.size());
    }
    catch (IOException e1) {
      System.out.println(this.getDatasetPath() +" not found");

      HashSet<Paper> setPapers = new HashSet<>();
      JsonParser parser = new JsonParser();
      String id ="";
      String description = "";
      String title = "";
      
      
      try {
        System.out.println("Parsing "+filename);
        FileReader read = new FileReader(filename);
        JsonArray docs = parser.parse(read).getAsJsonObject().get("response").getAsJsonObject().getAsJsonArray("docs");
        long starttime = System.currentTimeMillis();

        for(int i = 0; i< docs.size(); i++){

          JsonObject doc = new JsonObject();
          if(docs.get(i).isJsonObject()){
            doc = docs.get(i).getAsJsonObject();

            id = doc.get("id").getAsString();

            description = doc.get("dc_description").getAsString();
            title = doc.get("dc_title").getAsString();

            List<String> language = languageDetector.getLanguages(description);
            if(language != null && !language.isEmpty()){
              if(language.get(0).equals("en")){
                System.out.println(language.toString());

                Paper paper = new Paper();
                paper.setId(id);
                paper.setTitle(title);
                List<String> toAnalyze = new ArrayList<String>();
                toAnalyze.add(title);
                
                if(!description.equals("null")){
                  toAnalyze.add(description);
                  ArrayList<Keyword> keys = this.extractKeywords(toAnalyze);
                  if(keys != null){
                    //System.out.println(id);
                    paper.setKeywordList(keys);
                    //System.out.println(keys);
                    if(!this.listPapers.contains(paper))
                      this.listPapers.add(paper);
                  }
                }

                System.out.println("Document "+i);
              }
            }
          }
        }

        long endtime = System.currentTimeMillis();
        long time = (endtime - starttime)/1000;
        System.out.println("Time "+time+" sec");
        System.out.println(listPapers.size());
        this.mapper.writerWithDefaultPrettyPrinter().writeValue(new File(this.getDatasetPath()), this.listPapers);
      }
      catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }


  }

  /**
   *Read the dataset in json format and creare the data stuctures for the papers 
   *Substitutes the deprecated function parse file
   * @param toParse
   *
   */

  public void parseDatasetFromJson(String filename){
    System.out.println("Read dataset: "+filename);
    ObjectMapper mapper = new ObjectMapper();

    try{

      this.listPapers = mapper.readValue(new File(filename), new TypeReference<ArrayList<Paper>>(){});


      ArrayList<Paper> tmpListpaper = new ArrayList<>();
      tmpListpaper.addAll(listPapers);

      for(Paper p: tmpListpaper){
        if(p.getKeywordList()==null || p.getKeywordList().isEmpty())
          listPapers.remove(p);
      }



      for(Paper paper: listPapers){
        if(paper.getCategoryList()!=null && !paper.getCategoryList().isEmpty()){
          //creo l'hashmap tra categorie e paper
          for(CategoriesResult c : paper.getCategoryList()){
            if(categoryMap.containsKey(c.getLabel())){
              //aggiungo il paper alla lista della mappa
              ArrayList<Paper> local = categoryMap.get(c.getLabel());
              local.add(paper);
              categoryMap.put(c.getLabel(), local);
            }else{
              ArrayList<Paper> local = new ArrayList<>();
              local.add(paper);
              categoryMap.put(c.getLabel(), local);
            }
          }

          HashSet<Keyword> keySet = new HashSet<Keyword>();

          if(paper.getKeywordList() != null){
            keySet.addAll(paper.getKeywordList());
          }
        }
      }
      System.out.println("Completed");
    }catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
  }



  /**
   * Old function that read from a file mixed xml and json
   * @param toParse
   * @deprecated 
   */
  @Deprecated
  public void parseFile(String toParse){
    this.toParse = new File(toParse);
    try{
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(this.toParse);
      //System.out.println(doc);
      doc.getDocumentElement().normalize();
      NodeList documents = doc.getElementsByTagName("paper");
      JsonParser jparser = new JsonParser();
      Paper paper = null;

      for(int i = 0; i<documents.getLength();i++){
        Node document = documents.item(i);
        paper = new Paper();
        NodeList childList = document.getChildNodes();

        for(int j = 0; j<childList.getLength();j++){
          Node n = childList.item(j);
          if(n.getNodeName().equals("id"))
            paper.setId(n.getTextContent());
          else if(n.getNodeName().equals("title"))
            paper.setTitle(n.getTextContent());

          try{
            if(n.getNodeName().equals("result")){
              //parsing json degli oggetti
              JsonObject ob =  jparser.parse(n.getTextContent()).getAsJsonObject();

              JsonArray categories = ob.get("categories").getAsJsonArray();
              ArrayList<CategoriesResult> categoriesList = new ArrayList<>();

              for(int k = 0; k < categories.size(); k++){
                CategoriesResult cresult = new CategoriesResult();
                JsonObject categoryOb = categories.get(k).getAsJsonObject();
                cresult.setLabel(categoryOb.get("label").getAsString());
                cresult.setScore(categoryOb.get("score").getAsDouble());
                categoriesList.add(cresult);
              }

              JsonArray keywords = ob.get("keywords").getAsJsonArray();
              ArrayList<Keyword> keywordList = new ArrayList<>();
              //costruisco l'array list di keyword
              Keyword key = null;
              for(int k = 0; k< keywords.size(); k++){
                key = new Keyword();
                key.setRelevance(keywords.get(k).getAsJsonObject().get("relevance").getAsDouble());
                Sentiment s = new Sentiment();
                s.setScore(keywords.get(k).getAsJsonObject().get("sentiment").getAsJsonObject().get("score").getAsDouble());
                key.setSentiment(s);
                key.setText(keywords.get(k).getAsJsonObject().get("text").getAsString());
                keywordList.add(key);

              }

              JsonArray concepts = ob.get("concepts").getAsJsonArray();
              ArrayList<Concept> conceptList = new ArrayList<>();
              //costruisco l'array list di concetti

              for(int k = 0; k < concepts.size();k++){
                Concept c = new Concept();
                c.setText(concepts.get(k).getAsJsonObject().get("text").getAsString());
                c.setRelevance(concepts.get(k).getAsJsonObject().get("relevance").getAsDouble());
                c.setDbpedia(concepts.get(k).getAsJsonObject().get("dbpedia_resource").getAsString());
                conceptList.add(c);
              }

              //aggiungo le liste al paper
              paper.setConceptList(conceptList);
              paper.setKeywordList(keywordList);
              paper.setCategoryList(categoriesList);

              //creo l'hashmap tra categorie e paper
              for(CategoriesResult c : paper.getCategoryList()){
                if(categoryMap.containsKey(c.getLabel())){
                  //aggiungo il paper alla lista della mappa
                  ArrayList<Paper> local = categoryMap.get(c.getLabel());
                  local.add(paper);
                  categoryMap.put(c.getLabel(), local);
                }else{
                  ArrayList<Paper> local = new ArrayList<>();
                  local.add(paper);
                  categoryMap.put(c.getLabel(), local);
                }
              }
            }
          }catch (Exception e) {
            // TODO: handle exception
            continue;
          }
        }
        if(!listPapers.contains(paper)){
          listPapers.add(paper); 
        }
        HashSet<Keyword> keySet = new HashSet<Keyword>();
        if(paper.getKeywordList() != null){
          keySet.addAll(paper.getKeywordList());
        }
      }
    }catch(Exception e){
      e.printStackTrace();
    }

  }






  public ArrayList<Paper> getListPapers(){
    return listPapers;
  }

  /**
   * String is the taxomy of the paper alias category
   * @param xmlFile
   * @return
   */
  public HashMap<String,ArrayList<Paper>> getCategoryMap(){
    return categoryMap;
  }

  public static String getDatasetPath() {
    return datasetPath;
  }

  public void setDatasetPath(String datasetPath) {
    PaperBuilder.datasetPath = datasetPath;
  }

  public static HashSet<String> returnAllKeywords(ArrayList<Paper> paperList){
    HashSet<String> keywordList = new HashSet<String>();
    for(Paper p : paperList){
      for(Keyword k : p.getKeywordList()){
        keywordList.add(k.getText());
      }
    }
    return keywordList;
  }







}

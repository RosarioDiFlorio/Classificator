package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

/**
 * Questa classe serve per creare il dataset da documenti Solr. Prende in input un file che contiene id di documenti, divisi per categorie.
 * @author lomasto
 * @author Rosario
 */
public  class DatasetBuilder {



  private ArrayList<Source> listSources;
  private HashMap<String,ArrayList<Source>> categoryMap;

  private KeywordExtractor keywordExtractor;
  private SolrClient solrClient;
  private ObjectMapper mapper;
  private TxtDataReader dataReader;
  private String fileName;

  /**
   * Constructor
   * As default instanziate a KeywordExtractor as InnenExtractor
   */
  public DatasetBuilder(){
    listSources = new ArrayList<Source>();
    categoryMap = new HashMap<>();
    mapper = new ObjectMapper();
    solrClient = new SolrClient();
    keywordExtractor = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
  }

  public DatasetBuilder(KeywordExtractor ke){
    listSources = new ArrayList<Source>();
    categoryMap = new HashMap<>();
    mapper = new ObjectMapper();
    solrClient = new SolrClient();
    keywordExtractor = ke;
  }


  /**
   * This class create the listOfSource taking documents from Solr
   * @param fileName (.txt), in this file are written the ids of sources by category usually is the training dataset.
   * @param path in the folder that contains the file.
   * @return
   * @throws IOException
   */
  public List<Source>  buildDataset(String fileName, String path, String category,boolean withCategory) throws IOException{  
    dataReader = new TxtDataReader(category,fileName,path);
    List<String> listIdPaper = new ArrayList<>(dataReader.getIds());  
    listSources.addAll(solrClient.getSourcesFromSolr(listIdPaper,Paper.class));
    if(withCategory)
      listSources = (ArrayList<Source>) addCategories(listSources);   
    listSources = (ArrayList<Source>) addKeywords(listSources);
    
    String simpleName =  fileName.replaceAll("\\.[a-zA-Z]*", "");   
    this.mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path+"/"+simpleName+".json"), this.listSources);
   
    
    return listSources;

  }



  public List<Source> addKeywords(ArrayList<Source> list) {
    ArrayList<Source> toRemove = new ArrayList<>();
    int count = 0;
    for(Source p: list){
     
      ArrayList<String> toAnalyze = new ArrayList<>();
      //toAnalyze.add(toAnalyze.toString());
      toAnalyze.add(p.getTexts().get(0)+"\n"+p.getTexts().get(1));
      try {   
        if(keywordExtractor.extractKeywordsFromTexts(toAnalyze,Configurator.numKeywords).get(0) != null){
          p.setKeywordList((ArrayList<Keyword>) keywordExtractor.extractKeywordsFromTexts(toAnalyze,Configurator.numKeywords).stream().flatMap(l->l.stream()).collect(Collectors.toList()));
        }
        else{
          toRemove.add(p);
        }
      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if(count%10==0)
        System.out.println("building dataset -> "+(Math.round((count*100)/list.size()))+"%");
      count++;
    }
    System.out.println("building dataset -> "+(Math.round((count*100)/list.size()))+"%");
    for(Source p:toRemove){
      list.remove(p);
    }
    
    return list;
  }

  /**
   *Read the dataset in json format and creare the data stuctures for the papers 
   * @param toParse
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   *
   */
  public ArrayList<Source> parseDatasetFromJson(String filename) throws JsonParseException, JsonMappingException, IOException{
    System.out.println("Read dataset: "+filename);
    ObjectMapper mapper = new ObjectMapper();

    this.listSources = mapper.readValue(new File(filename), new TypeReference<ArrayList<Source>>(){});


    ArrayList<Source> tmpListSources = new ArrayList<>();
    tmpListSources.addAll(listSources);

    for(Source p: tmpListSources){
      if(p.getKeywordList()==null || p.getKeywordList().isEmpty())
        listSources.remove(p);
    }

    for(Source source: listSources){
      if(source.getCategoryList()!=null && !source.getCategoryList().isEmpty()){
        //creo l'hashmap tra categorie e paper
        for(CategoriesResult c : source.getCategoryList()){
          if(categoryMap.containsKey(c.getLabel())){
            //aggiungo il paper alla lista della mappa
            ArrayList<Source> local = categoryMap.get(c.getLabel());
            local.add(source);
            categoryMap.put(c.getLabel(), local);
          }else{
            ArrayList<Source> local = new ArrayList<>();
            local.add(source);
            categoryMap.put(c.getLabel(), local);
          }
        }
      }
    }

    return listSources;
  }


  public static HashSet<String> returnAllKeywords(ArrayList<Source> sourceList){
    HashSet<String> keywordList = new HashSet<String>();
    for(Source p : sourceList){
      for(Keyword k : p.getKeywordList()){
        keywordList.add(k.getText());
      }
    }
    return keywordList;
  }

  public List<Source> addCategories(List<Source> list) throws IOException{
    
    Map<String, HashMap<String, String>> categoryPapers = dataReader.categoriesWithIds(dataReader.getFileToReadSource(),dataReader.getFileToReadCategory());
    for(Source paper :list){
      ArrayList<CategoriesResult> categoriesForCurrentPaper = new ArrayList<>();
      //PER IL PAPER CORRENTE MI PRENDO LE CATEGORIE ALLE QUALI APPARTIENE, CICLO PER LE CATEGORIE
      for(String key : categoryPapers.keySet()){
        if(categoryPapers.get(key).keySet().contains(paper.getId())){
          if(categoryPapers.get(key).get(paper.getId())!=null && !categoryPapers.get(key).get(paper.getId()).isEmpty()){
            CategoriesResult category = new CategoriesResult();
            category.setLabel(key.toLowerCase());
            try{
              category.setScore(Double.parseDouble(categoryPapers.get(key).get(paper.getId())));
            }
            catch(Exception e){
              System.out.println(categoryPapers.get(key).get(paper.getId()));
            }
            categoriesForCurrentPaper.add(category);
          }
        }
      }
      paper.setCategoryList(categoriesForCurrentPaper);
    }
    return list;
  }


  public ArrayList<Source> getSourceList() {
    return listSources;
  }

  public void setSourceList(ArrayList<Source> listPapers) {
    this.listSources = listPapers;
  }

  public KeywordExtractor getKeywordExtractor() {
    return keywordExtractor;
  }

  public void setKeywordExtractor(KeywordExtractor keywordExtractor) {
    this.keywordExtractor = keywordExtractor;
  }

  public String getFileName() {
    return fileName;
  }


  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public static void saveSources(List<Source> listSource,String pathFile) throws JsonGenerationException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathFile), listSource);
    System.out.println("source list save to "+pathFile);
  }

  public static List<Source> loadSources(String pathFile) throws JsonParseException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    List<Source> toReturn = new ArrayList();
    toReturn = mapper.readValue(new File(pathFile), new TypeReference<List<Source>>(){});
    return toReturn;
  }
  
  public static Map<String,Source> loadMapSources(String pathFileJson) throws JsonParseException, JsonMappingException, IOException{
    Map<String,Source> mapSources = new HashMap<>();
    List<Source> sources = loadSources(pathFileJson);
    for(Source s: sources){
      mapSources.put(s.getId(), s);
    }
    return mapSources;
  }



  /**
   * Example Main
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    //db.buildDataset("training_results.txt",PathConfigurator.applicationFileFolder);    

  }








}

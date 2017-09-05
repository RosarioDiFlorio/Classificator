package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.prepocessing.interfaces.DataReader;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

/**
 * Questa classe serve per creare il dataset da documenti Solr. Prende in input un file che contiene id di documenti, divisi per categorie.
 * @author lomasto
 *
 */

public  class DatasetBuilder {



  private ArrayList<Source> listSources;
  private HashMap<String,ArrayList<Source>> categoryMap;

  private KeywordExtractor keywordExtractor;
  private SolrClient solrClient;
  private ObjectMapper mapper;
  private DataReader dataReader;
  private String fileName;

  public DatasetBuilder(){
    listSources = new ArrayList<Source>();
    categoryMap = new HashMap<>();
    mapper = new ObjectMapper();
    solrClient = new SolrClient();
  }


  public void  buildDataset(String fileName, String path) throws IOException{
    
    dataReader = new TxtDataReader(fileName,path);
    List<String> listIdPaper = new ArrayList<>(dataReader.getIds());
    
    listSources.addAll(solrClient.getSourcesFromSolr(listIdPaper,Paper.class));
    
    listSources = (ArrayList<Source>) addCategories(listSources);
    listSources = (ArrayList<Source>) addKeywords(listSources);

    String simpleName =  fileName.replaceAll("\\.[a-zA-Z]*", "");   
    listSources.stream().forEach(p->p.setDescription(null));
    this.mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path+"/"+simpleName+".json"), this.listSources);
  
   
  }



  public List<Source> addKeywords(ArrayList<Source> list) {
    for(Source p: list){
      try {
        p.setKeywordList((ArrayList<Keyword>) keywordExtractor.extractKeywordsFromText(p.getTexts(),4));
      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
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


    ArrayList<Source> tmpListpaper = new ArrayList<>();
    tmpListpaper.addAll(listSources);

    for(Source p: tmpListpaper){
      if(p.getKeywordList()==null || p.getKeywordList().isEmpty())
        listSources.remove(p);
    }

    for(Source paper: listSources){
      if(paper.getCategoryList()!=null && !paper.getCategoryList().isEmpty()){
        //creo l'hashmap tra categorie e paper
        for(CategoriesResult c : paper.getCategoryList()){
          if(categoryMap.containsKey(c.getLabel())){
            //aggiungo il paper alla lista della mappa
            ArrayList<Source> local = categoryMap.get(c.getLabel());
            local.add(paper);
            categoryMap.put(c.getLabel(), local);
          }else{
            ArrayList<Source> local = new ArrayList<>();
            local.add(paper);
            categoryMap.put(c.getLabel(), local);
          }
        }
      }
    }
    
    return listSources;
  }
  
  
  public static HashSet<String> returnAllKeywords(ArrayList<Source> paperList){
    HashSet<String> keywordList = new HashSet<String>();
    for(Source p : paperList){
      for(Keyword k : p.getKeywordList()){
        keywordList.add(k.getText());
      }
    }
    return keywordList;
  }

  public List<Source> addCategories(List<Source> list) throws IOException{
    Map<String, HashMap<String, String>> categoryPapers = dataReader.categoriesWithIds();

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



  /**
   * Example Main
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException{
    DatasetBuilder db = new DatasetBuilder();
    
    //KeywordExtractor keywordExtractor = new MauiExtractor("../KeywordExtractor/", "none", "newInnenModel");
    KeywordExtractor keywordExtractor = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    db.setKeywordExtractor(keywordExtractor);
    db.buildDataset("trainAndTestTogether.txt",PathConfigurator.trainingAndTestFolder);    
    
  }








}

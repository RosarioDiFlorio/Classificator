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
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.maui.main.MauiExtractor;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.prepocessing.interfaces.DataReader;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;



public  class DatasetBuilder {



  private ArrayList<Paper> listPapers;
  private HashMap<String,ArrayList<Paper>> categoryMap;

  private KeywordExtractor keywordExtractor;
  private SolrClient solrClient;
  private ObjectMapper mapper;
  private DataReader dataReader;
  private String fileName;

  public DatasetBuilder(){
    listPapers = new ArrayList<Paper>();
    categoryMap = new HashMap<>();
    mapper = new ObjectMapper();
    solrClient = new SolrClient();
  }


  public void buildDataset() throws IOException{
    dataReader = new TxtDataReader(this.fileName);
    List<String> listIdPaper = new ArrayList<>(dataReader.getIds());
    listPapers.addAll(solrClient.getPapersFromSolr(listIdPaper));
    listPapers = (ArrayList<Paper>) addCategories(listPapers);
    listPapers = (ArrayList<Paper>) addKeywords(listPapers);

    this.mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.datasetFolder+"backup/"+fileName+"_complete.json"), this.listPapers);;    
    listPapers.stream().forEach(p->p.setDescription(null));
    this.mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.datasetFolder+"TrainingAndTest/"+fileName+".json"), this.listPapers);
  }



  public List<Paper> addKeywords(ArrayList<Paper> list) {
    for(Paper p: list){
      List<String> toAnalyze = new ArrayList<String>();

      toAnalyze.add(p.getTitle());
      toAnalyze.add(p.getDescription());
      try {
        p.setKeywordList((ArrayList<Keyword>) keywordExtractor.extractKeywordsFromText(toAnalyze));
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
   *Substitutes the deprecated function parse file
   * @param toParse
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   *
   */
  public void parseDatasetFromJson(String filename) throws JsonParseException, JsonMappingException, IOException{
    System.out.println("Read dataset: "+filename);
    ObjectMapper mapper = new ObjectMapper();

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
      }
    }
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

  public List<Paper> addCategories(List<Paper> list) throws IOException{
    Map<String, HashMap<String, String>> categoryPapers = dataReader.categoriesWithIds();

    for(Paper paper :list){
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


  public ArrayList<Paper> getListPapers() {
    return listPapers;
  }

  public void setListPapers(ArrayList<Paper> listPapers) {
    this.listPapers = listPapers;
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
    db.setFileName("tmp");
    KeywordExtractor keywordExtractor = new MauiExtractor("../KeywordExtractor/", "none", "newInnenModel");

    db.setKeywordExtractor(keywordExtractor);
    db.buildDataset();
  }








}

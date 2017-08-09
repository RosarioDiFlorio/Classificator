package eu.innovation.engineering.prepocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.util.Paper;
import eu.innovation.engineering.prepocessing.util.SolrClient;



public  class DatasetBuilder {

  private String datasetFolder = "data/datasets/";
  private ArrayList<Paper> listPapers;
  private KeywordExtractor keywordExtractor;
  private SolrClient solrClient;
  private ObjectMapper mapper;
  
  
  private String fileName="";



  public DatasetBuilder(){
    mapper = new ObjectMapper();
    solrClient = new SolrClient();
  }

  
//METODO CHE RESTITUISCE UNA LISTA DI CATEGORIE; PER OGNI CATEGORIA UNA LISTA DI ID DI PAPER CON RELEVANCE
  private HashMap<String, HashMap<String, String>> categoriesWithPaper() throws IOException {
    FileReader reader = new FileReader(fileName+".txt");
    BufferedReader bufferedReader = new BufferedReader(reader);
    String line = bufferedReader.readLine();
    HashMap<String,HashMap<String,String>> categoryPapers = new HashMap<>();
    HashMap<String,String> paperIntoCurrentCategory = null;
    boolean firstcategory = true;
    String currentCategory="";
    while(line!=null){
      //STO LEGGENDO LA PRIMA CATEGORIA
      if(line.contains("/") && firstcategory){
        currentCategory = line;
        firstcategory = false;
        paperIntoCurrentCategory = new HashMap<>();
      }
      //STO LEGGENDO UNA CATEGORIA CHE NON E' LA PRIMA
      else if(line.contains("/") && !firstcategory){
        categoryPapers.put(currentCategory, paperIntoCurrentCategory);
        currentCategory = line;
        paperIntoCurrentCategory = new HashMap<>();
      }
      //STO LEGGENDO UN PAPER
      else if(line.contains("_")){
        String split[] = line.split(" ");
        paperIntoCurrentCategory.put(split[0], split[1]);
      }

      line = bufferedReader.readLine();
    }
    //SALVO ANCHE L?ULTIMA CATEGORIA
    categoryPapers.put(currentCategory, paperIntoCurrentCategory);

    for(String category : categoryPapers.keySet()){
      System.out.println(category+" "+categoryPapers.get(category).size());
    }

    return categoryPapers;
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

  public String getDatasetFolder() {
    return datasetFolder;
  }

  public void setDatasetFolder(String datasetFolder) {
    this.datasetFolder = datasetFolder;
  }




  


}

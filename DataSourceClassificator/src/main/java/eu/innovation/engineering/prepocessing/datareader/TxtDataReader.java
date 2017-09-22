package eu.innovation.engineering.prepocessing.datareader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.interfaces.DataReader;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

public class TxtDataReader implements DataReader {

  private static String fileToReadSource = "";
  private static String fileToReadCategory = "";

  public static void main(String[] args) throws Exception{
    TxtDataReader reader = new TxtDataReader("trainingAndTestTogether.txt", PathConfigurator.trainingAndTestFolder,"");

    String pathFile1 = PathConfigurator.trainingAndTestFolder+"trainingAndTestTogether.txt";
    String pathFile2 = PathConfigurator.trainingAndTestFolder+"trainingDatasetFromCsvResult.txt";
    String pathWhereSave = PathConfigurator.trainingAndTestFolder+"trainingDatasetMerged.txt";
    int limitSource = 70;
    reader.mergeTxtDataset(pathFile1, pathFile2, limitSource, pathWhereSave, "categories.txt");
  }

  public TxtDataReader(){
    
  }
  
  public TxtDataReader(String category, String filename, String path) {
    this.fileToReadSource = path + filename;
    this.fileToReadCategory = path + category;
  }

  


  /**
   * This method read the simple txt file and return the ids under every category
   */
  @Override
  public Set<String> getIds() throws IOException {
    FileReader reader = new FileReader(fileToReadSource);
    BufferedReader bufferedReader = new BufferedReader(reader);
    String line = bufferedReader.readLine();
    Set<String> idPapers = new HashSet<>();

    while(line!=null){
      if(line.contains("_")){
        String splitLine[] = line.split(" ");
        idPapers.add(splitLine[0]);
      }
      line = bufferedReader.readLine();
    }
    return idPapers;
  }

  public void mergeTxtDataset(String pathFile1, String pathFile2,int limitSource, String pathWhereSave, String pathCategories) throws IOException{
    
    Map<String, HashMap<String, String>> mapFile1 = categoriesWithIds(pathFile1,pathCategories);
    System.out.println(pathFile1);

    Map<String, HashMap<String, String>> mapFile2 = categoriesWithIds(pathFile2,pathCategories);
    System.out.println(pathFile2);
    PrintWriter p = new PrintWriter(new File(pathWhereSave));
    for(String cf1: mapFile1.keySet()){
      p.println(cf1);
      Set<String> ids2 = new HashSet<>();
      Set<String> ids1 = new HashSet<>(mapFile1.get(cf1).keySet());
      
      if(mapFile2.keySet().contains(cf1)) {
        ids2 = new HashSet<>(mapFile2.get(cf1).keySet());
      }
      ids1.addAll(ids2);
      
      int countSource = 0;
      for(String id: ids1){       
        if (countSource>= limitSource) {
          break;
        }
        countSource++;
        p.println(id+" 1");
      }
      
      p.println();
      p.flush();
    }
    p.close();
  }


  public void checkCategory(String pathFile,String category,boolean withTexts,String pathCategories) throws Exception{
    KeywordExtractor kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    SolrClient solr = new SolrClient();

    List<String> ids = new ArrayList<>();
    ids.addAll(categoriesWithIds(pathFile,pathCategories).get("/"+category.replace("_", " ")).keySet());
    List<Source> sources = solr.getSourcesFromSolr(ids, Paper.class);      

    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationTestFolder+category+"ToCheck.txt"));
    for(Source src: sources){
      p.println(src.getId()+" - "+category);
      p.println(src.getTitle());
      p.println(kex.extractKeywordsFromTexts(src.getTexts(), 10).stream().flatMap(l->l.stream()).map(k->k.getText()).collect(Collectors.toList())+"\n");

      if(withTexts)
        src.getTexts().stream().forEach(p::println);
      p.println("--------------------------------------\n");
      p.flush();
    }
    p.close();
  }

  /**
   * This method create an HashMap contained as key the category 
   * and as value an HashMap with key ids of the document and as value the relevance
   * @param string 
   */
  @Override
  public Map<String, HashMap<String, String>> categoriesWithIds(String pathFileSource, String pathFileCategories) throws IOException {

    
    FileReader reader = new FileReader(pathFileSource);

    BufferedReader bufferedReader = new BufferedReader(reader);
    String line = bufferedReader.readLine();

    List<String> categories = getCategories(pathFileCategories);
    HashMap<String,HashMap<String,String>> categoryPapers = new HashMap<>();
    HashMap<String,String> paperIntoCurrentCategory = null;

    String currentCategory="";
    while(line!=null){
      if(categories.contains(line)){
        currentCategory = line;
        paperIntoCurrentCategory = new HashMap<>();
      }
      else{
        String split[] = line.split(" ");
        
        if(split!=null && split.length>1){
          paperIntoCurrentCategory.put(split[0], split[1]);
        }
      }
      line = bufferedReader.readLine();
      categoryPapers.put(currentCategory, paperIntoCurrentCategory);
    }
    categoryPapers.put(currentCategory, paperIntoCurrentCategory);
    for(String category : categoryPapers.keySet()){
      System.out.println(category+" "+categoryPapers.get(category).size());
    }
    bufferedReader.close();
    return categoryPapers;
  }
 
  public static List<String> getCategories(String pathFileCategories) throws IOException{
    List<String> categories = new ArrayList<>();
    FileReader fr = new FileReader(pathFileCategories);
    BufferedReader bufferedReader = new BufferedReader(fr);
    String line = bufferedReader.readLine();
    int index = 0;
    while(line!= null){
      categories.add(index, line);
      index++;
      line=bufferedReader.readLine();
    }
    return categories;
  }

  public String getFileToReadSource() {
    return fileToReadSource;
  }

  public void setFileToReadSource(String fileToRead) {
    this.fileToReadSource = PathConfigurator.applicationFileFolder + fileToRead;
  }
  
  public  String getFileToReadCategory() {
    return fileToReadCategory;
  }

  
  public  void setFileToReadCategory(String fileToReadCategory) {
    TxtDataReader.fileToReadCategory = fileToReadCategory;
  }

}

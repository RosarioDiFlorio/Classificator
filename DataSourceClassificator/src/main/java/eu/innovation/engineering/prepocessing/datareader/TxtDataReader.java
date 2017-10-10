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

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.interfaces.DataReader;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

public class TxtDataReader implements DataReader {

  private static String fileToReadSource = "";
  private static String fileToReadCategory = "";


  public static void main(String[] args) throws Exception{
    TxtDataReader datareader = new TxtDataReader();
    datareader.checkCategory(PathConfigurator.rootFolder+"trainingMerged.txt", PathConfigurator.rootFolder+"categories.txt", "food and drink");
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
  
  public Set<String> getIds(String filePath) throws IOException {
    setFileToReadSource(filePath);
    return getIds();
  }


  public void mergeTxtDataset(String pathOldSet, String pathNewSet, String pathWhereSave, int limitSource, String pathCategories) throws IOException{
    System.out.println("##############\nMerging Set 1 -> "+pathOldSet+" AND Set 2 -> "+pathNewSet+"\n############");
    Map<String, HashMap<String, String>> mapFileOld = categoriesWithIds(pathOldSet,pathCategories);
    Map<String, HashMap<String, String>> mapFileNew = categoriesWithIds(pathNewSet,pathCategories);

    Set<String> allCategories = new HashSet<>();
    allCategories.addAll(mapFileOld.keySet());
    allCategories.addAll(mapFileNew.keySet());

    PrintWriter p = new PrintWriter(new File(pathWhereSave));
    for(String category: allCategories){
      p.println(category);
      Set<String> ids = new HashSet<>();
      
      if(mapFileOld.containsKey(category))
        ids.addAll(mapFileOld.get(category).keySet());
      if(mapFileNew.containsKey(category))
        ids.addAll(mapFileNew.get(category).keySet());

      int countSource = 0;
      for(String id: ids){       
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


  public void checkCategory(String pathFile,String pathCategories,String categoryFilter) throws Exception{
    SolrClient solr = new SolrClient();

    List<String> ids = new ArrayList<>();
    ids.addAll(categoriesWithIds(pathFile,pathCategories).get(categoryFilter).keySet());
    List<Source> sources = solr.getSourcesFromSolr(ids, Paper.class);      

    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationFileFolder+"checkedCategory"));
    p.println("###########################\n"+categoryFilter.toUpperCase()+"\n##########################\n");
    for(Source src: sources){
        p.println(src.getId());
        p.println(src.getTitle()+"\n");
        p.println(src.getDescription());
        p.println("------------------------------------------\n");
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
    System.out.println("sources path -> "+pathFileSource);
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
    System.out.println("-------------------------------------\n");
    bufferedReader.close();
    return categoryPapers;
  }
  

  public static List<String> getCategories(String pathFileCategories) throws IOException{
    List<String> categories = new ArrayList<>();
    System.out.println("categories path -> "+pathFileCategories);
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
    this.fileToReadSource =  fileToRead;
  }

  public  String getFileToReadCategory() {
    return fileToReadCategory;
  }


  public  void setFileToReadCategory(String fileToReadCategory) {
    TxtDataReader.fileToReadCategory = fileToReadCategory;
  }

}

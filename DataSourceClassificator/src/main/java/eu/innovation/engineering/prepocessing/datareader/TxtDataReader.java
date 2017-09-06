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

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.interfaces.DataReader;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

public class TxtDataReader implements DataReader {

  private static String fileToRead = "test";

  public static void main(String[] args) throws Exception{
    TxtDataReader reader = new TxtDataReader("trainingAndTestTogether.txt", PathConfigurator.trainingAndTestFolder);
    for(int i = 0; i< Configurator.Categories.values().length;i++){
      reader.checkCategory(Configurator.Categories.values()[i].name(),false);
    }
  }

  public TxtDataReader(String filename, String path) {
    this.fileToRead = path + filename;
  }

  /**
   * This method read the simple txt file and return the ids under every category
   */
  @Override
  public Set<String> getIds() throws IOException {
    FileReader reader = new FileReader(fileToRead);
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

  public void checkCategory(String category,boolean withTexts) throws Exception{
    KeywordExtractor kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    SolrClient solr = new SolrClient();

    List<String> ids = new ArrayList<>();
    ids.addAll(categoriesWithIds().get("/"+category.replace("_", " ")).keySet());
    List<Source> sources = solr.getSourcesFromSolr(ids, Paper.class);      

    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationTestFolder+category+"ToCheck.txt"));
    for(Source src: sources){
      p.println(src.getId()+" - "+category);
      p.println(src.getTitle());
      p.println(kex.extractKeywordsFromText(src.getTexts(), 10).stream().map(k->k.getText()).collect(Collectors.toList())+"\n");
      
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
   */
  @Override
  public Map<String, HashMap<String, String>> categoriesWithIds() throws IOException {

    FileReader reader = new FileReader(fileToRead);

    BufferedReader bufferedReader = new BufferedReader(reader);
    String line = bufferedReader.readLine();

    Set<String> categories = Configurator.getCategories();
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
        if(split.length>1){
          paperIntoCurrentCategory.put(split[0], split[1]);
        }
      }
      line = bufferedReader.readLine();
      categoryPapers.put(currentCategory, paperIntoCurrentCategory);
    }
    //SALVO ANCHE L?ULTIMA CATEGORIA
    categoryPapers.put(currentCategory, paperIntoCurrentCategory);



    for(String category : categoryPapers.keySet()){
      System.out.println(category+" "+categoryPapers.get(category).size());
    }
    return categoryPapers;
  }

  public String getFileToRead() {
    return fileToRead;
  }

  public void setFileToRead(String fileToRead) {
    this.fileToRead = PathConfigurator.applicationFileFolder + fileToRead;
  }

}

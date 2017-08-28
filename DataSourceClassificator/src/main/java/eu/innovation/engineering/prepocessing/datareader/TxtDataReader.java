package eu.innovation.engineering.prepocessing.datareader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.innovation.engineering.prepocessing.interfaces.DataReader;
import eu.innovation.engineering.prepocessing.util.configurator.PathConfigurator;

public class TxtDataReader implements DataReader {

  private static String fileToRead = "test";


  public TxtDataReader(String filename) {
    this.fileToRead = filename;  
    this.fileToRead = PathConfigurator.applicationFileFolder + fileToRead;
  }

  /**
   * This method read the simple txt file and return the ids under every category
   */
  @Override
  public Set<String> getIds() throws IOException {
    FileReader reader = new FileReader(fileToRead+".txt");
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

  /**
   * This method create an HashMap contained as key the category 
   * and as value an HashMap with key ids of the document and as value the relevance
   */
  @Override
  public Map<String, HashMap<String, String>> categoriesWithIds() throws IOException {
    FileReader reader = new FileReader(fileToRead+".txt");
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

  public String getFileToRead() {
    return fileToRead;
  }

  public void setFileToRead(String fileToRead) {
    this.fileToRead = fileToRead;
  }

}
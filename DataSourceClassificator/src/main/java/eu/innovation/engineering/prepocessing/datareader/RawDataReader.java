package eu.innovation.engineering.prepocessing.datareader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.innovation.engineering.prepocessing.interfaces.DataReader;

public class RawDataReader implements DataReader {
  
  private String fileToRead = "data/rawtrain_test/test";
  
  
 
  public RawDataReader(String filename) {
    setFileToRead(filename);
  }

  
  @Override
  public Set<String> getIdPaper() throws IOException {
    FileReader reader = new FileReader(fileToRead+".txt");
    BufferedReader bufferedReader = new BufferedReader(reader);
    String line = bufferedReader.readLine();
    Set<String> idPapers = new HashSet<>();

    //LEGGO I PAPER DAL FILE
    while(line!=null){
      if(line.contains("_")){
        String splitLine[] = line.split(" ");
        idPapers.add(splitLine[0]);
      }
      line = bufferedReader.readLine();
    }
    return idPapers;
  }

  @Override
  public Map<String, HashMap<String, String>> categoriesWithPaper() throws IOException {
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

package eu.innovation.engineering.graph.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class StopWordEnglish {
  
  private static Set<String> stopWords;
  
  public StopWordEnglish(String stopWordFolder){
    initStopWords(stopWordFolder+"/stopwords_en.txt");
  }
  
  public boolean isStopWord(String word){
    if(stopWords != null){
      if(stopWords.contains(word.toLowerCase()))
        return true;
      else 
        return false;
    }else{
      System.out.println("stopWords not initialized");
      return false;
    }
  }
  
  
  
  private void initStopWords(String stopWordPath){
    stopWords = new HashSet<String>();
    File txt = new File(stopWordPath);  
    InputStreamReader is;
    String sw = null;
    try {
      is = new InputStreamReader(new FileInputStream(txt), "UTF-8");
      BufferedReader br = new BufferedReader(is);             
      while ((sw=br.readLine()) != null)  {
        stopWords.add(sw.toLowerCase());   
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public  Set<String> getStopWords(String stopWordPath){
    if (stopWords == null) {
      initStopWords(stopWordPath);
    }
    return stopWords;
  }


}

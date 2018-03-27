package eu.innovationengineering.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StopWords {
  
  private Set<String> stopWords;
  
  public StopWords(String lang){  
    try {
      initStopWords("stopwords/stopwords_"+lang+".txt");
    }
    catch (IOException e) {
      e.printStackTrace();
    }
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
  
  
  
  private void initStopWords(String stopWordPath) throws IOException{
    try (BufferedReader file = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(stopWordPath), "UTF-8"))) {
      stopWords = file.lines().collect(Collectors.toSet());
    }
  }
  
  public  Set<String> getStopWords(String stopWordPath){
    return stopWords;
  }
 
  public List<String> cleanText(String text){
    text = text.replaceAll("\\p{Punct}", " ");
    text = text.replaceAll("\\d+", " ");
    text = text.replace("/", " ");
    return Arrays.asList(text.split(" ")).stream().filter(el->!isStopWord(el) && !el.matches("")).map(el->el.toLowerCase().trim()).collect(Collectors.toList());
  }

}

package eu.innovation.engineering.test;

import java.io.IOException;
import java.util.List;

import eu.innovation.engineering.wikipedia.WikipediaMiner;

public class Test {

  
  
  public static void main (String[] args) throws IOException{
    String queryKey = "waste management";
    List<String> pages = WikipediaMiner.searchWiki(queryKey.trim());
    String category = "";
    int count = 0;
    boolean isAcceptable = false;
    while(!isAcceptable){
      category = WikipediaMiner.getCategory(pages.get(count),queryKey);
      if(category.contains("Category:Disambiguation pages"))
        count++;
      else
        isAcceptable = true;
    }
    System.out.println(category);
  }
}

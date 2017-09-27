package eu.innovation.engineering.util.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.util.CleanUtilis;
import eu.innovation.engineering.keyword.extractor.util.LanguageDetector;

/**
 * 
 * @author lomasto
 *This class is use to evaluate text or description. The analyzer methods return true or false to accept or to reject.
 *This class use two metrics to evaluate the text : 
 *  -language (English) 
 *  -length (without stop-words)    
 *
 */

public class TextValidator {



  public static void main(String[] args){

    TextValidator tx = new TextValidator(40);
    
    System.out.println(tx.analyzer("September 1, 2012 4 pages, 2 figures, to appear in the Proceedings of Moriond Cosmology 2012"));

  }

  
  
  private LanguageDetector languageDetector = null;
  private CleanUtilis cleaner = null;
  private int minSize;
  
  public TextValidator(int num){
    this.languageDetector = new LanguageDetector();
    this.cleaner = new CleanUtilis();
    this.minSize=num;
  }



  public boolean analyzer (String text){

    text = text.toLowerCase();
    if(length(text)&&language(text))
      return true;
    else
      return false;

  }

  private boolean language(String text) {
    ArrayList<String> list = new ArrayList<String>();
    list.add(text);
    List<String> result = this.languageDetector.filterForLanguage(list, "en");
    if(result!=null && result.size()>0)
      if(result.get(0).equals(text))
      return true;

    return false;
  }

  private boolean length(String text) {
    Set<String> stopWords = cleaner.getBlackList(PathConfigurator.keywordExtractorsFolder+"data/stopwords/stopwords_en.txt");
    
    
    ArrayList<String> wordInText = new ArrayList<>(Arrays.asList(text.toLowerCase().split(" ")));
    
    for(String stopWord : stopWords){
        wordInText.remove(stopWord);
    }
    
    
    
    System.out.println(wordInText.size());
    if(wordInText.size()>=this.minSize)
      return true;
    
    return false;
  }





}

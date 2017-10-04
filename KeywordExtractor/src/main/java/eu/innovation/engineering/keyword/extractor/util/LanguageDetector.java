package eu.innovation.engineering.keyword.extractor.util;

import java.util.ArrayList;
import java.util.List;

import eu.innovationengineering.language.detector.impl.CybozuLanguageDetector;

public class LanguageDetector {

  private CybozuLanguageDetector languageDetector;

  public LanguageDetector(){
    languageDetector = new CybozuLanguageDetector();
    languageDetector.init();
  }


  public List<String> filterForLanguage(List<String> texts, String langFilter) {
    List<String> toReturn= new ArrayList<>();
    try {
      for(String text:texts){
        List<String> langRes = languageDetector.getLanguages(text);
        if(!langRes.isEmpty()){
          if(langRes.get(0).equals(langFilter)){
            toReturn.add(text);
          }
        }
      }
    }catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return toReturn;
  }

  public boolean isValidLanguage(String text,String langFilter) throws Exception{
    List<String> langRes = languageDetector.getLanguages(text);
    if(!langRes.isEmpty()){
      if(langRes.get(0).equals(langFilter)){
        return true;
      }
    }
    return false;
  }  

}

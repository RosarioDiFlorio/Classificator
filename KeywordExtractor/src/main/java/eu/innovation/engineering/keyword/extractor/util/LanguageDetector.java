package eu.innovation.engineering.keyword.extractor.util;

import java.util.List;

import eu.innovationengineering.language.detector.impl.CybozuLanguageDetector;

public class LanguageDetector {
  
  private CybozuLanguageDetector languageDetector;
  
  public LanguageDetector(){
    languageDetector = new CybozuLanguageDetector();
    languageDetector.init();
  }

  
  public String filterForLanguage(List<String> texts, String langFilter) {
    String toReturn= "";
    try {
      for(String text:texts){
        List<String> langRes = languageDetector.getLanguages(text);
        if(!langRes.isEmpty()){
          if(langRes.get(0).equals(langFilter)){
            toReturn += text + "\n";
          }
        }
      }
    }catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return toReturn;
  }
}

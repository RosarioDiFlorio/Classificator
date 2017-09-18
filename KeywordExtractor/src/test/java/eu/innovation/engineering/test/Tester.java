package eu.innovation.engineering.test;

import eu.innovationengineering.lang.ISO_639_1_LanguageCode;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;

public class Tester {
  
  
  
  public static void main(String[] args) throws LanguageException{
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();
    nlpAnalyzer.detectSentences("test", ISO_639_1_LanguageCode.ENGLISH);
    nlpAnalyzer.executeCompleteSentenceAnalysis("test", ISO_639_1_LanguageCode.ENGLISH, true, true);
    
  }
  


}

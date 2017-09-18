package eu.innovation.engineering.test;

import java.util.List;

import eu.innovation.engineering.LSA.keywordExtractor.LSAKeywordExtractor;
import eu.innovation.engineering.keyword.extractor.util.CleanUtilis;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;
import eu.innovationengineering.nlp.beans.oat.SentenceChunk;

public class Tester {



  public static void main(String[] args) throws LanguageException{
    //init 
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();


    String test= "he first person in line for Hillary Clinton’s book signing in New York said he had not voted at all in the presidential election – and that he regretted it."
        +"Brian Maisonet, a 29-year-old from Brooklyn, said he had arrived at 3.30pm on Monday and waited outside the bookstore overnight to meet Clinton at a Tuesday afternoon event for her book What Happened, a punchy and personal account of her stunning defeat by Donald Trump.";    

    String[] cleanedString = test.split(" ");
    test = "";
    for(String s : cleanedString){
      if(!CleanUtilis.getBlackList().contains(s))
        test+= s+" ";
    }
    
    List<SentenceChunk> result = LSAKeywordExtractor.createChunkFromText(test);

    //System.out.println(result);


  }



}

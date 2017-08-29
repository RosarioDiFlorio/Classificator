package eu.innovationengineering.Tester;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.maui.main.MauiExtractor;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.util.preprocessing.Source;

/**
 * Hello world!
 *
 */
public class KeywordExtractorTester 
{

  public static void main(String args[]) throws Exception{
    PrintWriter writer = new PrintWriter(new File("results.txt"));
    KeywordExtractor innenExtractor = new InnenExtractor("../KeywordExtractor/");


    DatasetBuilder db = new DatasetBuilder();

    db.parseDatasetFromJson("source/test_complete.json");
    ArrayList<Source> listaSource = db.getListPapers();

    for(Source source : listaSource){
      writer.println(source.getId()+"\n");
      writer.println(source.getTitle()+"\n");
      writer.println("INNEN EXTRACTOR");
      List<Keyword> keywordsResultInnen = innenExtractor.extractKeywordsFromText(source.getTexts());
      int count =0;
      for(Keyword k :keywordsResultInnen){
        if(count<5){
          count++;
        writer.println(k.getText()+"("+k.getRelevance()+")");
        }
        else
          break;
        
      }

      writer.println("\nMAUI EXTRACTOR");
      KeywordExtractor mauiExtractor = new MauiExtractor("../KeywordExtractor/", "none", "newInnenModel");

      List<Keyword> keywordsResultInnenMaui =  mauiExtractor.extractKeywordsFromText(source.getTexts());

      for(Keyword k :keywordsResultInnenMaui){
        writer.println(k.getText()+"("+k.getRelevance()+")");
      }
      
      writer.println("\n-------------------------------------------");
    }
    
    writer.flush();
    writer.close();
  }

}

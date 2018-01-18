package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class AppTest 
extends TestCase
{

  public static void main(String[] args) throws IOException{
    main2(args);
  }
  
  
  public static void main2(String[] args) throws IOException{
    String word = "";
    List<List<String>> toVectorize = new ArrayList<>();
    toVectorize.add(cleanText("1988_establishments_in_Ivory_Coast"));
    toVectorize.add(cleanText("1980s_establishments_in_Ivory_Coast"));
    float[][] results = Word2Vec.returnVectorsFromTextList(toVectorize);
    System.out.println(CrawlerWikipediaCategory.cosineSimilarityInverse(results[0], results[1]));
  }
  private static List<String> cleanText(String text){
    StopWordEnglish stopWords = new StopWordEnglish("stopwords_en.txt");
    text = text.replace("_", " ");
    return Arrays.asList(text.split(" ")).stream().filter(el->!stopWords.isStopWord(el)).map(el->el.toLowerCase()).collect(Collectors.toList());
  }
  public static void main1(String[] args) throws IOException{
    //    System.err.println(analyzer.getDocumentLabelsBFS("9912937"));
    List<String> labelsList = AnalyzerWikipediaGraph.getDocumentLabelsDijstra("9912937");
    System.out.println(labelsList);
  }

}

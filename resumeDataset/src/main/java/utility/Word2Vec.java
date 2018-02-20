package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import datasetCreatorFromTaxonomy.ResumeDataset.AnalyzerWikipediaGraph;
import datasetCreatorFromTaxonomy.ResumeDataset.CrawlerWikipediaCategory;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovationengineering.word2vec.common.Constants;
import eu.innovationengineering.word2vec.common.request.bean.VectorListRequestBean;
import eu.innovationengineering.word2vec.service.rest.impl.Word2vecServiceImpl;

public class Word2Vec {

  private static WebClient webClient;
  private static Word2vecServiceImpl word2vecService;

  public static float[][] returnVectorsFromTextList(List<List<String>> textList) throws IOException{


    VectorListRequestBean vectorListRequest = new VectorListRequestBean();
    vectorListRequest.setDocs(textList);
    //chiamo il wordToVec per calcolare il vettore delle stinghe ottenute
    if(webClient == null)
      webClient = WebClient.create("http://smartculture-projects.innovationengineering.eu/word2vec-rest-service/", Arrays.asList(new JacksonJaxbJsonProvider()));

    if(word2vecService == null)
      word2vecService = new Word2vecServiceImpl();
    try{      
      word2vecService.setWebClient(webClient);
      return word2vecService.getVectorList(Constants.GENERAL_CORPUS, Constants.ENGLISH_LANG, vectorListRequest);
    }
    catch(Exception e){
      System.out.println(e);
      return null;
    }
  }

  public static void main(String[] args){
    try {
      // FileReader reads text files in the default encoding.
      FileReader fileReader = 
          new FileReader("restest.txt");

      // Always wrap FileReader in BufferedReader.
      BufferedReader bufferedReader = 
          new BufferedReader(fileReader);
      String cls = "agri-foodstuffs/animal product/beverages and sugar/plant product";

     Map<String,Set<String>> glossary = new HashMap<>();
     Set<String> words = new HashSet<>();
      String line;
      while((line = bufferedReader.readLine()) != null) {
        if(line.length()>2){
          List<String> tmp = AnalyzerWikipediaGraph.cleanText(line.toLowerCase());
          if(!tmp.isEmpty()){
            if(tmp.get(0).length()>2){
              String word = tmp.get(0);
              List<List<String>> toVectorize = new ArrayList<>();
              toVectorize.add(AnalyzerWikipediaGraph.cleanText(cls));
              toVectorize.add(AnalyzerWikipediaGraph.cleanText(word));
              float[][] vectorized = Word2Vec.returnVectorsFromTextList(toVectorize);
              double res = CrawlerWikipediaCategory.cosineSimilarity(vectorized[0], vectorized[1]);
              if(res >0.3){
                System.out.println(word);
                words.add(word);
              }

            }
          }


        }
      }   
      glossary.put("food", words);
      ObjectMapper writer = new ObjectMapper();
      writer.writerWithDefaultPrettyPrinter().writeValue(new File("food.json"),glossary);

      // Always close files.
      bufferedReader.close();         
    }catch (Exception e) {
      // TODO: handle exception
    }

  }


  public static void main1(String[] args) throws Exception{
    Word2Vec word2vec = new Word2Vec();

    /*String test = "We are a simple successful Pan European Commercial Bank, with a fully plugged in CIB, delivering a unique Western, Central and Eastern European network to our extensive client franchise: 25 million clients."+
        "We  offer local expertise as well as international reach. We accompany and support our 25 million clients globally, providing them with unparalleled access to our leading banks in 14 core markets as well as to an another 18 countries worldwide."+
        "Our European banking network includes Italy, Germany, Austria, Bosnia and Herzegovina, Bulgaria, Croatia, Czech Republic, Hungary, Romania, Russia, Slovakia, Slovenia, Serbia and Turkey."+
        "Our strategic position in Western and Central and Eastern Europe enables us to command one of highest market shares in the region."+
        "While our brand is recognizable all over Europe, we have preserved the highly valuable local brands of banks that we acquired to form our Group.";
     */
    /*String test = "The company’s founders, Gottlieb Daimler and Carl Benz, made history with the invention of the automobile in the year 1886. 125 years later, in anniversary year 2011, Daimler AG is one of the world’s most successful automotive companies. With its divisions Mercedes-Benz Cars, Daimler Trucks, Mercedes-Benz Vans, Daimler Buses and Daimler Financial Services, the Daimler Group is one of the biggest producers of premium cars and the world’s biggest manufacturer of commercial vehicles with a global reach. Daimler Financial Services provides its customers with a full range of automotive financial services including financing, leasing, insurance and fleet management."
        +"As an automotive pioneer, Daimler continues to shape the future of mobility. The Group applies innovative and green technologies to produce safe and superior vehicles which fascinate and delight its customers. With the development of alternative drive systems, Daimler is the only vehicle producer investing in all three technologies of hybrid drive, electric motors and fuel cells, with the goal of achieving emission-free mobility in the long term. This is just one example of how Daimler willingly accepts the challenge of meeting its responsibility towards society and the environment."
        +"Daimler sells its vehicles and services in nearly all the countries of the world and has production facilities in Europe, North and South America, Asia, and Africa. Its current brand portfolio includes, in addition to the world’s most valuable premium automotive brand, Mercedes-Benz, as well as Mercedes-AMG and Mercedes-Maybach, the brands smart, Freightliner, Western Star, BharatBenz, Fuso, Setra and Thomas Built Buses, and Daimler Financial Services’ brands: Mercedes-Benz Bank, Mercedes-Benz Financial, Daimler Truck Financial, moovel and car2go. The company is listed on the stock exchanges of Frankfurt and Stuttgart (stock exchange symbol DAI).";
     */
    String test = "Mercedes-Benz (German: [mɛʁˈtseːdəsˌbɛnts]) is a global automobile marque and a division of the German company Daimler AG. The brand is known for luxury vehicles, buses, coaches, and lorries. The headquarters is in Stuttgart, Baden-Württemberg. The name first appeared in 1926 under Daimler-Benz.Mercedes-Benz traces its origins to Daimler-Motoren-Gesellschaft's 1901 Mercedes and Karl Benz's 1886 Benz Patent-Motorwagen, which is widely regarded as the first gasoline-powered automobile.";
    //String test = "Nucleic acid hybridization studies were made between 71S-AMV-RNA and DNA from leukemic myeloblasts and from normal chicken cells. There was homology between the viral RNA and chicken cell DNA and to a greater extent between viral RNA and leukemic cell DNA. Leukemic cell DNA hybridized approximately twice as much viral RNA as did normal chicken DNA. Thermal melting studies showed that the viral RNA bound to normal and leukemic cell DNA consists of long polynucleotides (Tm = 87° and 92°C, respectively, in 2× saline citrate). This suggests that the leukemic cells contain a DNA template of the viral RNA.";
    //String test = "Previous work indicated that the light chains of a monotypic immunoglobulins G2-K and M-K from a single patient (Ti1) are identical. Our present data show that the monotypic immunoglobulins G and M share idiotypic determinants not present in their isolated light chains or in any of a large number of other immunoglobulins tested, and that amino acid sequences of the first 27 residues from the NH2-terminal end of the γ- and μ-chains are identical. These results support the hypothesis that at least two genes control the synthesis of each heavy and light chain and suggest that the monotypic immunoglobulin G and monotypic immunoglobulin M of this patient share three of the four genes involved. It is proposed that, during normal immunoglobulin synthesis, different cells of a single clone synthesize immunoglobulins M and G, and that the light chains and the variable segments of the heavy chains of the proteins of the two classes are identical within the clone. A genetic switching mechanism is suggested.";

    List<String> testList = new ArrayList<>();
    testList.add(test);

    String[] classes = {
        "finance/free_movement_of_capital/capital_movement/",
        //        "trade/marketing/",
        //        "trade/international_trade/"
        "transport/land_transport/",
        "economics/economic_analysis/economic_indicator/",
        "economics/economic_structure/economic_system/"
        //        "production_technology_and_research/technology_and_technical_regulations/technology/nanotechnology/",
        //        "production_technology_and_research/technology_and_technical_regulations/technology/technological_change/"
        //        "science/medical_science/immunology/",
        //        "science/natural_and_applied_sciences/life_sciences/biology/",
        //        "science/natural_and_applied_sciences/life_sciences/ecology/",
        //        "science/natural_and_applied_sciences/applied_sciences/cybernetics/",
    };
    KeywordExtractor ke = new InnenExtractor("../KeywordExtractor/");
    List<String> keywords = ke.extractKeywordsFromTexts(testList, 4).get(0).stream().map(k->k.getText()).collect(Collectors.toList());
    System.out.println(keywords);

    for(String classToCompare : classes){
      List<List<String>> toVectorize = new ArrayList<>();
      toVectorize.add(AnalyzerWikipediaGraph.cleanText(classToCompare));
      toVectorize.add(keywords);
      float[][] vectorized = word2vec.returnVectorsFromTextList(toVectorize);
      double sum = 0;
      sum+=CrawlerWikipediaCategory.cosineSimilarity(vectorized[0], vectorized[1]);

      System.out.print(classToCompare +" = "+sum);
      boolean flag = false;
      if(sum >0.2){
        flag = true;
      }
      System.out.println(" - "+flag);
      toVectorize.clear();
    }

  }



}

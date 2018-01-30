package eu.innovation.engineering.graph.utility;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import eu.innovationengineering.word2vec.common.Constants;
import eu.innovationengineering.word2vec.common.request.bean.VectorListRequestBean;
import eu.innovationengineering.word2vec.service.rest.impl.Word2vecServiceImpl;

public class Word2Vec {

  private static WebClient webClient;
  private static Word2vecServiceImpl word2vecService;
  private static StopWordEnglish stopWords;
  public float[][] returnVectorsFromTextList(List<List<String>> textList) throws IOException{


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
  
  public static List<String> cleanText(String text){
    if(stopWords == null)
      stopWords = new StopWordEnglish("stopwords_en.txt");
    text = text.replaceAll("\\p{Punct}", " ");
    text = text.replaceAll("\\d+", " ");
    return Arrays.asList(text.split(" ")).stream().filter(el->!stopWords.isStopWord(el) && !el.matches("")).map(el->el.toLowerCase().trim()).collect(Collectors.toList());
  }
}

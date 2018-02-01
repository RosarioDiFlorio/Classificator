package eu.innovation.engineering.graph.utility;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import eu.innovationengineering.word2vec.common.Constants;
import eu.innovationengineering.word2vec.common.request.bean.VectorListRequestBean;
import eu.innovationengineering.word2vec.service.Word2vecService;

public class Word2Vec {
  @Autowired
  private StopWordEnglish stopWords;
  
  @Autowired
  private Word2vecService word2vecService;
  
  public float[][] returnVectorsFromTextList(List<List<String>> textList) throws IOException{
    VectorListRequestBean vectorListRequest = new VectorListRequestBean();
    vectorListRequest.setDocs(textList);
    //chiamo il wordToVec per calcolare il vettore delle stinghe ottenute
    return word2vecService.getVectorList(Constants.GENERAL_CORPUS, Constants.ENGLISH_LANG, vectorListRequest);
  }
  
  public List<String> cleanText(String text){
    text = text.replaceAll("\\p{Punct}", " ");
    text = text.replaceAll("\\d+", " ");
    return Arrays.asList(text.split(" ")).stream().filter(el->!stopWords.isStopWord(el) && !el.matches("")).map(el->el.toLowerCase().trim()).collect(Collectors.toList());
  }
}

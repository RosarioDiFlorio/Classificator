package eu.innovation.engineering.start;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.preprocessing.Source;
import eu.innovationengineering.word2vec.common.Constants;
import eu.innovationengineering.word2vec.common.request.bean.VectorListRequestBean;
import eu.innovationengineering.word2vec.service.rest.impl.Word2vecServiceImpl;


public class MatrixCosineSimilarity {

  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
    DatasetBuilder db = new DatasetBuilder();
    
    ArrayList<Source> sourceList = db.parseDatasetFromJson(PathConfigurator.trainingAndTestFolder+"train.json");
    
    double[][] matrix = new double[sourceList.size()][sourceList.size()];
    
    ArrayList<Keyword> keywordList = new ArrayList<>();
    for(Source s : sourceList){
      keywordList.addAll(s.getKeywordList());
    }
    
    
    int count = 0;
    for(Keyword k1 : keywordList){
      float[] vectorK1 = fromTextToVector(k1.getText())[0];
      for(Keyword k2 : keywordList){
        float[] vectorK2 = fromTextToVector(k2.getText())[0];
        double similarity = FeatureExtractor.cosineSimilarity(vectorK1, vectorK2);
        System.out.println(k1.getText()+" "+k2.getText()+" "+similarity);
      }
    }
    
  }
  
  
  public static float[][] fromTextToVector(String keyword) throws IOException {

    List<List<String>> keywordList = new ArrayList<List<String>>();
    ArrayList<String> toConvert = new ArrayList<>();
    toConvert.add(keyword);
    keywordList.add(toConvert);
    

    VectorListRequestBean vectorListRequest = new VectorListRequestBean();
    vectorListRequest.setDocs(keywordList);


    WebClient webClient = WebClient.create("http://smartculture-projects.innovationengineering.eu/word2vec-rest-service/", Arrays.asList(new JacksonJaxbJsonProvider()));

    try (Word2vecServiceImpl word2vecService = new Word2vecServiceImpl()) {      
      word2vecService.setWebClient(webClient);
      float[][] vectorListKeyword = word2vecService.getVectorList(Constants.GENERAL_CORPUS, Constants.ENGLISH_LANG, vectorListRequest);
      return vectorListKeyword ;

    }

  }

}

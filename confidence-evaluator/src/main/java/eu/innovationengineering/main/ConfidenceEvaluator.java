package eu.innovationengineering.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovationengineering.extractor.Keyword;
import eu.innovationengineering.extractor.LSACosineKeywordExtraction;
import eu.innovationengineering.services.ConfidenceRequest;
import eu.innovationengineering.services.ConfidenceRequester;
import eu.innovationengineering.services.ConfidenceResponse;
import eu.innovationengineering.utilities.Similarities;
import eu.innovationengineering.utilities.SpringMainLauncher;
import eu.innovationengineering.utilities.StopWords;
import eu.innovationengineering.utilities.Word2Vec;

public class ConfidenceEvaluator extends SpringMainLauncher implements ConfidenceRequester, InitializingBean {


  @Autowired
  private StopWords stopWords;

  @Autowired
  private LSACosineKeywordExtraction kex;

  private ObjectMapper mapper;

  @Autowired
  private Word2Vec word2Vec;


  private String volumeFolder;

  private Map<String,float[]> categoriesVector;


  public ConfidenceEvaluator(String volumeFolder) throws JsonParseException, JsonMappingException, IOException{
    this.volumeFolder = volumeFolder+"/";
    this.mapper = new ObjectMapper();
  }


  public static void main(String[] args) throws Exception{
    mainWithSpring(
        context -> {
          ConfidenceEvaluator evaluator = context.getBean(ConfidenceEvaluator.class);


          String test = "     A powered exoskeleton includes a leg part and a foot part.            The foot part includes a foot bottom plate and a foot bottom plate driving wheel.            The foot bottom plate includes a first surface and a second surface opposite to each other; and the foot bottom plate driving wheel is connected below the leg part.            The foot bottom plate is movably connected to the foot bottom plate driving wheel and capable of being turned over relative to the foot bottom plate driving wheel.            In the walking mode, the foot bottom plate is turned over till the first surface of the foot bottom plate faces upward and is connected below the foot bottom plate driving wheel; and in the wheeled movement mode, the foot bottom plate is turned over till a second surface of the foot bottom plate faces upward and is connected above the foot bottom plate driving wheel.";        
          List<String> classListTest = new ArrayList<>();
          classListTest.add("home_and_garden");
          classListTest.add("industry");
          ConfidenceRequest request = new ConfidenceRequest();
          request.setClassList(classListTest);
          request.setText(test);
          System.out.println( evaluator.computeConfidence(request));


        },
        args,
        "classpath:spring/properties-config.xml","classpath:spring/word2vec.xml" ,
        "classpath:spring/service-config.xml","classpath:spring/utilities-config.xml",
        "classpath:spring/persistence-config.xml");
  }


  private Map<String,float[]> getCategoriesVector() throws JsonParseException, JsonMappingException, IOException{
    Map<String,float[]> toReturn = new HashMap<>();
    Map<String,List<String>> categoriesMap = mapper.readValue(new File(volumeFolder+"categories.json"), new TypeReference<Map<String,List<String>>>() {});

    List<String> supportSet = new ArrayList<String>(categoriesMap.keySet());
    for(String key : categoriesMap.keySet()){
      supportSet.addAll(categoriesMap.get(key));
    }
    List<List<String>> toVectorize = new ArrayList<>();
    for(String element:supportSet){
      toVectorize.add(stopWords.cleanText(element));
    }
    float[][] vectorized = word2Vec.returnVectorsFromTextList(toVectorize);
    for(int i = 0;i<vectorized.length;i++){
      toReturn.put(supportSet.get(i), vectorized[i]);
    }
    return toReturn;
  }

  @Override
  public ConfidenceResponse computeConfidence(ConfidenceRequest request) throws Exception {
    ConfidenceResponse response = new ConfidenceResponse();
    List<String> classList = request.getClassList();
    String text = request.getText();

    Set<String> categories = new HashSet<>();

    for(String res: classList){
      categories.addAll(Arrays.asList(res.split("/")));
    }

    List<String> testList = new ArrayList<>();
    testList.add(text);
    List<Keyword> keys = kex.extractKeywordsFromTexts(testList, categories, 10).get(0);
    if(keys != null){
      List<List<String>> toVectorize = new ArrayList<>();
      StringBuilder sb = new StringBuilder();
      for(Keyword key : keys){
        sb.append(key.getText()+" ");
      }
      toVectorize.add(stopWords.cleanText(sb.toString()));
      float[] keyVectors = word2Vec.returnVectorsFromTextList(toVectorize)[0];
      toVectorize.clear();


      List<ConfidenceResult> confidences = new ArrayList<>();
      for(String cat:classList){
        String[] str = cat.split("/");
        float[] catVect = this.categoriesVector.get(str[str.length-1]);
        double sim = Similarities.cosineSimilarity(keyVectors, catVect);
        confidences.add(new ConfidenceResult(cat, sim));

      }
      Collections.sort(confidences);
      Collections.sort(keys);

      
      response.setStatus(200);
      response.setMessage("Task completed");
      response.setConfidences(confidences);
      response.setKeywords(keys);
      return response;
    }else{
      System.out.println(keys);
      response.setStatus(500);
      response.setMessage("keyword Extraction failed");
      response.setConfidences(null);
      response.setKeywords(null);
      return response;
    }
  }


  @Override
  public void afterPropertiesSet() throws Exception {
    this.categoriesVector = getCategoriesVector();
  }








}

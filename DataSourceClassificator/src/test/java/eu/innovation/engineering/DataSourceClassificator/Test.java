package eu.innovation.engineering.DataSourceClassificator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.LSA.keywordExtractor.LSACosineKeywordExtraction;
import eu.innovation.engineering.LSA.keywordExtractor.LSAKeywordExtractor;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

public class Test {


  
  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException{
    
    String workingPath = PathConfigurator.rootFolder+"chemistry biology"+"/";
    String glossaryName = workingPath+"glossaries.json";
    
    List<Source> sources = DatasetBuilder.loadSources(workingPath+"test.json"); 
    Set<String> keywordSet = new HashSet<String>();
    sources.stream().map(s->s.getKeywordList().stream().map(k->k.getText()).collect(Collectors.toList())).forEach(keywordSet::addAll);
       
    ObjectMapper mapper = new ObjectMapper();
    Map<String,List<String>> glossaryMap = mapper.readValue(new File(glossaryName),new TypeReference<Map<String,List<String>>>() {});
    
    
    for(String key : glossaryMap.keySet()){
      Set<String> tmp = keywordSet;
      tmp.retainAll(glossaryMap.get(key));
      System.out.println(key+"\n"+tmp.toString());
    }
    
    
    
    for(String key: glossaryMap.keySet()){
      Set<String> glossarySet = new HashSet<>(glossaryMap.get(key));
      System.out.println("intersection of "+key);
      for(String k:glossaryMap.keySet()){
        if(!k.equals(key)){
          Set<String> tmp = new HashSet<>(glossaryMap.get(k));
          tmp.retainAll(glossarySet);
          System.out.println("with "+k+" "+tmp);

          
        }
      }
      Set<String> tmp = new HashSet<>(glossarySet);
      tmp.retainAll(keywordSet);
      System.out.println("keywords intersection "+tmp);
      System.out.println();
    }
    
    
    ClusteringKMeans cluster = new ClusteringKMeans();
    List<List<String>> textList = new ArrayList<>();
    List<String> list = new ArrayList<>();
    list.add("haplotype");
    textList.add(list);
    list = new ArrayList<>();
    list.add("business"); 
    textList.add(list);
    list = new ArrayList<>();
    list.add("biology");
    textList.add(list);
    
    
    float[][] vectors = cluster.returnVectorsFromTextList((ArrayList<List<String>>) textList);
    
    System.out.println(Arrays.toString(vectors[0]));
    System.out.println(FeatureExtractor.cosineSimilarity(vectors[0], vectors[1]));
    System.out.println(FeatureExtractor.cosineSimilarity(vectors[0], vectors[2]));

    
  }
  
  

  public static void main1(String[] args) throws Exception{
    /*
    System.out.println(DatasetBuilder.loadSources(PathConfigurator.applicationFileFolder+"sources.json").size());
    System.out.println(SourceVectorBuilder.loadSourceVectorList(PathConfigurator.applicationFileFolder+"sourceVectors.json").size());
    System.out.println(DatasetBuilder.loadSources(PathConfigurator.applicationFileFolder+"dictionariesCategory/science/trainingScience.json").size());
     */
    KeywordExtractor lsaKe = new LSAKeywordExtractor(PathConfigurator.keywordExtractorsFolder);
    KeywordExtractor innKe = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    LSACosineKeywordExtraction lsaCosKe = new LSACosineKeywordExtraction(PathConfigurator.keywordExtractorsFolder,PathConfigurator.applicationFileFolder+"glossaries.json");






    SolrClient solrClient = new SolrClient();
    String id="10127141_163";
//    String id2 ="10028729_234";
//    String id3 = "10026237_234";



    List<String> ids = new ArrayList<>();
    ids.add(id);
//    ids.add(id3);
//    ids.add(id2);
    List<Source> sources = solrClient.getSourcesFromSolr(ids, Paper.class);
    
   long totalTime = 0;
    for(Source source: sources){
      long startTime = 0;
      String description = source.getTitle()+" "+source.getTexts().get(1);
      String fullText = source.getTexts().get(2);

      List<String> toAnalyze = new ArrayList<>();

      toAnalyze.add(description);
      //toAnalyze.add(fullText);

      /*
      startTime = System.currentTimeMillis();
      List<List<Keyword>> innResults = innKe.extractKeywordsFromTexts(toAnalyze, 5);
      System.out.println("INNEN finished -> " + (System.currentTimeMillis() - startTime));
       */

      startTime = System.currentTimeMillis();
      List<List<Keyword>> lsaCosResults = lsaCosKe.extractKeywordsFromTexts(toAnalyze, 5);
      totalTime += (System.currentTimeMillis() - startTime);
      System.out.println("LSACosine finished -> " + (System.currentTimeMillis() - startTime));
      
      System.out.println("ID -> "+source.getId());
      System.out.println("Title ->"+source.getTitle()+"\n");  
      //      System.out.println("INNEN ->"+innResults.get(0));
      //    System.out.println("LSA ->"+lsaResults.get(0));
      System.out.println("LSACosine -> "+lsaCosResults.get(0));
      System.out.println("--------------------------\n");
      
    }
    System.out.println("Total time ->" + totalTime);

  }
}

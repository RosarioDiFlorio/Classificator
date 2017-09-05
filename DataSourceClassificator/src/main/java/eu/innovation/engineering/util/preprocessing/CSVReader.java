package eu.innovation.engineering.util.preprocessing;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;

public class CSVReader {
  private static final int numKey = 10;

  public static void main(String[] args) throws Exception{
    float uThreshold = (float) 0.5;
    float lThreshold = 0;
    readResultClassifier(PathConfigurator.applicationFileFolder+"results2.csv",lThreshold,uThreshold);
  }

  public static void readResultClassifier(String csvFile, float lowThreshold,float upperThreshold) throws Exception{
    KeywordExtractor kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    Map<String, List<String>> dataMap = read(csvFile);

    List<String> ids = new ArrayList<>();
    ids.addAll(dataMap.keySet());

    SolrClient solr = new SolrClient();
    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationFileFolder+"filetocheck.txt"));

    for(String id: dataMap.keySet()){
      float probs = 0;
      try{
      probs = Float.parseFloat(dataMap.get(id).get(0));
      }catch (Exception e) {
        continue;
      }
      
      if(probs <= upperThreshold && probs >= lowThreshold){
        System.out.println(probs);
        List<String> tmp  = new ArrayList<>();
        tmp.add(id);

        List<Source> sources = solr.getSourcesFromSolr(tmp, Paper.class);
        
        
        for(Source s: sources){
          p.println(s.getId()+" - "+dataMap.get(s.getId()).get(0));
          p.println(kex.extractKeywordsFromText(s.getTexts(), numKey).stream().map(Keyword::getText).collect(Collectors.toList())+"\n");
          p.println(s.getTitle());
          p.println(s.getTexts().get(1));
          p.println("-------------------------------------\n");
        }
        p.flush();
      }   
    }
    p.close();
  }

  public static Map<String,List<String>> read(String csvFile) {


    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<String>> dataMap = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      while ((line = br.readLine()) != null) {

        // use comma as separator
        String[] csvData = line.split(cvsSplitBy); 
        List<String> data = new ArrayList<>();
        for(int i =1;i<csvData.length;i++){
          data.add(csvData[i]);
        }
        dataMap.put(csvData[0], data);
      }

      return dataMap;


    } catch (IOException e) {
      e.printStackTrace();
    }
    return dataMap;

  }

}

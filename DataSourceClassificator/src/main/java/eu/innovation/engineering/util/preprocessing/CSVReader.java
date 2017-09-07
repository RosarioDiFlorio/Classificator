package eu.innovation.engineering.util.preprocessing;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.Configurator.Categories;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;

public class CSVReader {
  private static final int numKey = 10;

  
  public static void main(String[] args) throws Exception{
    readResultFolders(PathConfigurator.applicationResultFolder, 200);
    //mainSingleTest(args);
    //mainToCreateTrainingDataset(args);
  }

  public static void mainSingleTest(String[] args) throws Exception{
    float uThreshold = (float) 1.0;
    float lThreshold = (float) 0.7;
    String category = Configurator.Categories.education.name();
    //category = "all";
    int batchLine = 0;
    boolean isCount = false;   
    
    
    int totalCount = 0;
    
    Categories[] categories = Configurator.Categories.values();  
    for(int i = 0; i<categories.length;i++){
      int countDocs = 0;
      category = categories[i].name();
      countDocs = readResultClassifier(PathConfigurator.applicationResultFolder+"test4/resultsNew500-1.csv",lThreshold,uThreshold,category,isCount,batchLine);
      System.out.println("category -> "+category);
      System.out.println("Founded sources -> "+countDocs);
      System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
      System.out.println("------------------------------------------------\n");
      totalCount += countDocs;
    }
    
    int countDocs = readResultClassifier(PathConfigurator.applicationResultFolder+"test4/resultsNew500-1.csv",lThreshold,uThreshold,category,isCount,batchLine);
    //System.out.println("category -> "+category);
    System.out.println("Total founded sources -> "+totalCount);
    //System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
    
    /*
    countDocs = readResultClassifier(PathConfigurator.applicationResultFolder+"test4/resultsNew500-2.csv",lThreshold,uThreshold,category,isCount,batchLine);
    System.out.println("category -> "+category);
    System.out.println("Founded sources -> "+countDocs);
    System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);

    countDocs = readResultClassifier(PathConfigurator.applicationResultFolder+"test4/resultsNew500-2.csv",lThreshold,uThreshold,category,isCount,batchLine);
    System.out.println("category -> "+category);
    System.out.println("Founded sources -> "+countDocs);
    System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
     */
    
  }
  
  
  public static void readResultFolders(String pathFolder, int clusterCut) throws Exception{
    int countDocs = 0;
    float uThreshold = (float) 1.0;
    float lThreshold = (float) 0.7;
    String category = Configurator.Categories.art_and_entertainment.name();
    category = "all";
    int batchLine = 0;
    boolean isCount = true;
    
    File folder = new File(pathFolder);
    File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isDirectory()) {
            System.out.println("Directory " + listOfFiles[i].getName());
            File subFolder = new File(listOfFiles[i].getAbsolutePath());
           
            File[] list = subFolder.listFiles();
            for (int j = 0; j < list.length; j++) {
              System.out.println(list[j].getName());
            }
            
          }
        }
 
    
  }
  
  
  



  public static int readResultClassifier(String csvFile, float lowThreshold,float upperThreshold,String category,boolean isCount,int batchLine) throws Exception{
    KeywordExtractor kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    Map<String, List<String>> dataMap = read(csvFile);

    List<String> ids = new ArrayList<>();
    ids.addAll(dataMap.keySet());

    SolrClient solr = new SolrClient();
    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationTestFolder+"filetocheck"+category+".txt"));
    int count = 0;
    String idToInsert= "";
    for(String id: dataMap.keySet()){
      float probs = 0;
      try{
        probs = Float.parseFloat(dataMap.get(id).get(0));
      }catch (Exception e) {
        e.printStackTrace();
        continue;
      }

      if(probs <= upperThreshold && probs >= lowThreshold ){      
        List<String> tmp  = new ArrayList<>();
        tmp.add(id);
        if(dataMap.get(id).get(1).contains(category) || category.equals("all")){

          if(count < batchLine || isCount){
            //System.out.println(count);
            count++;
            continue;
          }

          List<Source> sources = solr.getSourcesFromSolr(tmp, Paper.class);

          for(Source s: sources){
            count++;
            System.out.println(count+" - "+category);
            idToInsert += s.getId()+" 1\n";
            p.println(s.getId()+" - "+dataMap.get(s.getId()).get(0)+" - "+dataMap.get(s.getId()).get(1));
            p.println(kex.extractKeywordsFromText(s.getTexts(), numKey).stream().map(Keyword::getText).collect(Collectors.toList())+"\n");
            p.println(s.getTitle());
            p.println(s.getTexts().get(1));
            p.println("-------------------------------------\n");

          }
        }
        p.flush();
      }   
    }
    p.println("\n"+idToInsert);
    p.flush();
    p.close();
    return count;
  }


  public static void createTrainingSetFromCsvResults(String csvFile, float lowThreshold,float upperThreshold,int limitSource) throws FileNotFoundException{

    Map<String, List<String>> dataMap = read(csvFile);
    
    HashMap<String, List<String>> categoryMap = new HashMap<>();
    
    float probs = 0;
    for(String id : dataMap.keySet()){
      try{
        probs = Float.parseFloat(dataMap.get(id).get(0));
      }catch (Exception e) {
        e.printStackTrace();
        continue;
      }
      
      if(probs <= upperThreshold && probs >= lowThreshold ){
        String category = dataMap.get(id).get(1);
        if(categoryMap.containsKey(category)){
          List<String> tmp = categoryMap.get(category);
          tmp.add(id);
          categoryMap.put(category, tmp);
        }else{
          List<String> tmp = new ArrayList<>();
          tmp.add(id);
          categoryMap.put(category, tmp);
        }       
      }       
    }


    PrintWriter p = new PrintWriter(new File(PathConfigurator.trainingAndTestFolder+"trainingDatasetFromCsvResult.txt"));

    for(String category: categoryMap.keySet()){
      int countSource = 0;
      p.println("/"+category.replace("_", " "));
      for(String id: categoryMap.get(category)){ 
        if(countSource >= limitSource)
          break;
        countSource++;
        p.println(id+" 1");       
      }
      p.println("\n");
      p.flush();
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
            data.add(csvData[i].trim());
            
        }
        String key = csvData[0].trim();
        dataMap.put(key, data);
        
          
      }

      return dataMap;


    } catch (IOException e) {
      e.printStackTrace();
    }
    return dataMap;

  }

}

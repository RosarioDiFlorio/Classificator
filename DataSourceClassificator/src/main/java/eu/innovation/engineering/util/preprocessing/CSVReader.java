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
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;

public class CSVReader {
  private static final int numKey = 10;


  public static void main(String[] args) throws Exception{
    //createGenericDataset(PathConfigurator.applicationFileFolder+"datasetGeneric/");
    
    
    //mainToCreateDataset(args);
    mainToTest(args);
    
    TxtDataReader dataReader = new TxtDataReader();
    //dataReader.categoriesWithIds(PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt");
    
   
  }

  public static void mainToCreateDataset(String[] args) throws IOException{
    float uThreshold = (float) 1.0;
    float lThreshold = (float) 0.7;
    int limitSource = 70;
    String fileCsv = PathConfigurator.applicationFileFolder+"results.csv";
    String pathWhereSave = PathConfigurator.applicationFileFolder+"trainingDatasetFromCsv.txt";
    
    String oldDataset = PathConfigurator.applicationFileFolder+"trainingDatasetMergedIntegrated.txt";

    
    
    createTrainingSetFromCsvResults(fileCsv, lThreshold, uThreshold, limitSource,pathWhereSave);
    TxtDataReader txtReader = new TxtDataReader();
    txtReader.mergeTxtDataset(oldDataset, pathWhereSave, 70, PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt");
    
    /*txtReader.mergeTxtDataset(PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt", PathConfigurator.applicationFileFolder+"toIntegrate.txt",
        limitSource, PathConfigurator.applicationFileFolder+"trainingDatasetMergedIntegrated.txt");
    */
  }

  public static void mainToTest(String[] args) throws Exception{
    float uThreshold = (float) 1.0;
    float lThreshold = (float) 0.7;
    int batchLine = 0;   
    boolean isCount = false;   
    boolean all = false;

    String category = Configurator.Categories.religion_and_spirituality.name();
    String testFolderName=PathConfigurator.applicationFileFolder+"results.csv";
    File f = new File(testFolderName);
    
    if(f.isDirectory()){
      System.out.println("FOLDER -> "+testFolderName);
      File[] list = f.listFiles();
      if(list.length>0){
        for(int i = 0; i<list.length;i++){
          int totalCount = 0;
          if(list[i].isFile() && list[i].getName().contains(".csv")){
            File fileToAnalyze = list[i];
            System.out.println(fileToAnalyze.getName()+"\n");
            if(all){
              Categories[] categories = Configurator.Categories.values();  
              for(int j = 0; j<categories.length;j++){
                int countDocs = 0;
                category = categories[j].name();
                countDocs = readResultClassifier(fileToAnalyze,lThreshold,uThreshold,category,isCount,batchLine);
                System.out.println("category -> "+category);
                System.out.println("Founded sources -> "+countDocs);
                System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
                System.out.println("------------------------------------------------\n");
                totalCount += countDocs;
              }
              System.out.println("Total founded sources -> "+totalCount);
            }else{
              int countDocs = readResultClassifier(fileToAnalyze,lThreshold,uThreshold,category,isCount,batchLine);
              System.out.println("category -> "+category);
              System.out.println("Founded sources -> "+countDocs);
              System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
              System.out.println("------------------------------------------------\n");
              totalCount += countDocs;
            }      
            System.out.println("Total founded sources -> "+totalCount);
          }
          System.out.println("-----------------------------------------------------\n");
        }
      }
    }else if(f.isFile()&&f.getName().contains(".csv")){
      int totalCount = 0;
      System.out.println("FILE -> "+testFolderName);
      if(all){
        Categories[] categories = Configurator.Categories.values();  
        for(int j = 0; j<categories.length;j++){
          int countDocs = 0;
          category = categories[j].name();
          countDocs = readResultClassifier(f,lThreshold,uThreshold,category,isCount,batchLine);
          System.out.println("category -> "+category);
          System.out.println("Founded sources -> "+countDocs);
          System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
          System.out.println("------------------------------------------------\n");
          totalCount += countDocs;
        }
      }else{
        int countDocs = readResultClassifier(f,lThreshold,uThreshold,category,isCount,batchLine);
        System.out.println("category -> "+category);
        System.out.println("Founded sources -> "+countDocs);
        System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
        System.out.println("------------------------------------------------\n");
        totalCount += countDocs;
      }
      System.out.println("Total sources found -> " +totalCount);
    }
  }

  public static void createGenericDataset(String pathFolder) throws Exception{
    String line = "";
    String txtFile="";
    List<Source> listSource = new ArrayList<>();
    KeywordExtractor kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    File folder = new File(pathFolder);
    File[] listOfFiles = folder.listFiles();
    String debug ="";
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        System.out.println(listOfFiles[i].getName());
        txtFile = listOfFiles[i].getName();
        String fullPath = pathFolder+txtFile;
        try (BufferedReader br = new BufferedReader(new FileReader(fullPath))) {
          int count = 0;
          List<String> texts = new ArrayList<>();
          Source s = new Source();
          while ((line = br.readLine()) != null) {
            if(count == 0){
              s.setTitle(line);
              count++;
            }
            texts.add(line);
          }
          s.setTexts(texts);
          s.setId(txtFile);
          s.setKeywordList((ArrayList<Keyword>) kex.extractKeywordsFromText(texts, 10));
          listSource.add(s);
          debug+=s.getId()+"\n";
          debug+= s.getTitle()+"\n\n";
          debug+=s.getKeywordList().stream().map(k->k.getText()).collect(Collectors.toList()).toString()+"\n";
          debug +="---------------------------\n\n";

        }catch (Exception e) {
          e.printStackTrace();
        }           
      }
    }
    DatasetBuilder.save(listSource, PathConfigurator.trainingAndTestFolder+"TestGeneric.json");
    //System.out.println(debug);
  }

  public static int readResultClassifier(File csvFile, float lowThreshold,float upperThreshold,String category,boolean isCount,int batchLine) throws Exception{
    KeywordExtractor kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    Map<String, List<String>> dataMap = read(csvFile.getAbsolutePath());

    List<String> ids = new ArrayList<>();
    ids.addAll(dataMap.keySet());

    SolrClient solr = new SolrClient();
    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationTestFolder+category+"_"+csvFile.getName().replace(".csv", "")+".txt"));
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
      
      if((probs <= upperThreshold) && (probs >= lowThreshold) ){       
        List<String> tmp  = new ArrayList<>();
        tmp.add(id);
        if(dataMap.get(id).get(1).contains(category)){

          if(count < batchLine || isCount){
            count++;
            p.println(id+" 1");
            p.flush();
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


  public static void createTrainingSetFromCsvResults(String csvFile, float lowThreshold,float upperThreshold,int limitSource,String PathWhereSave) throws FileNotFoundException{

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


    PrintWriter p = new PrintWriter(new File(PathWhereSave));

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

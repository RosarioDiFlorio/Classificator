package eu.innovation.engineering.prepocessing.datareader;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.LSA.keywordExtractor.LSAKeywordExtractor;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

public class CSVDataReader {
  private static final int numKey = 4;
  private static final int limitSource = 100;


  public static void main(String[] args) throws Exception{

    //createGenericDataset(PathConfigurator.applicationFileFolder+"datasetGeneric/chemistry/");
    //mainToCreateDataset(args);
    mainToTest(args);

    //TxtDataReader dataReader = new TxtDataReader();
    //dataReader.categoriesWithIds(PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt");
    //dataReader.checkCategory(PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt", Configurator.Categories.society.name(), true);

  }

  public static void mainToCreateDataset(String[] args) throws IOException{
    float uThreshold = (float) 1.0;
    float lThreshold = (float) 0.7;
    String  categoryFilter = "";
    String fileCsv = PathConfigurator.applicationFileFolder+"results.csv";
    String pathWhereSave = PathConfigurator.applicationFileFolder+"trainingDatasetFromCsv.txt";
    createTxtTrainingFromCsvResults(fileCsv,lThreshold,uThreshold,pathWhereSave, categoryFilter);
    TxtDataReader txtReader = new TxtDataReader();
    //txtReader.mergeTxtDataset(oldDataset, pathWhereSave, 70, PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt");
  }

  public static void mainToTest(String[] args) throws Exception{
    String testFolderName=PathConfigurator.applicationFileFolder+"results/extractors/resultsLSA.csv";
    KeywordExtractor kex = new LSAKeywordExtractor(PathConfigurator.keywordExtractorsFolder);
    
    float uThreshold = (float) 1.0;
    float lThreshold = (float) 0.7;
    int batchLine = 0;   
    boolean isCount =  false;   
    boolean all = true;
    String batchCategory = "";
    String categoryFolder = "";
    String category = "finance";

    File f = new File(testFolderName);

    if(f.isDirectory()){
      System.out.println("FOLDER -> "+testFolderName);
      File[] list = f.listFiles();
      if(list.length>0){
        for(int i = 0; i<list.length;i++){
          int totalCount = 0;
          if(list[i].isFile() && list[i].getName().contains(".csv")){
            File fileToAnalyze = list[i];
            formatResultsFile(fileToAnalyze, kex, lThreshold, uThreshold, category, batchCategory, categoryFolder, isCount, batchLine, all);
          }
          System.out.println("-----------------------------------------------------\n");
        }
      }
    }else if(f.isFile()&&f.getName().contains(".csv")){
      formatResultsFile(f, kex, lThreshold, uThreshold, category, batchCategory, categoryFolder, isCount, batchLine, all);
    }
  }
  
  public static void formatResultsFile(File fileToAnalyze, KeywordExtractor kex, float lThreshold,float uThreshold,String category,String batchCategory,String categoryFolder,boolean isCount,int batchLine,boolean all) throws Exception{
    int totalCount = 0;
    System.out.println("FILE -> "+fileToAnalyze);
    if(all){
      List<String> categories = Collections.EMPTY_LIST;
      if(category.length()<1)
        categories = TxtDataReader.getCategories(PathConfigurator.rootFolder+"categories.txt");
      else
        categories = TxtDataReader.getCategories(PathConfigurator.rootFolder+categoryFolder+"/"+"categories.txt");  
      
      System.out.println(categories.size());
      
      for(int j = 0; j<categories.size();j++){
        int countDocs = 0;
        category = categories.get(j);
        if(category.equals(batchCategory) || batchCategory.equals("")){
          countDocs = readResultClassifier(fileToAnalyze,kex, lThreshold,uThreshold,category,isCount,batchLine);
          System.out.println("category -> "+category);
          System.out.println("Founded sources -> "+countDocs);
          System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
          System.out.println("------------------------------------------------\n");
          totalCount += countDocs;
          batchCategory = "";
          batchLine = 0;
        }
      }
    }else{
      int countDocs = readResultClassifier(fileToAnalyze,kex, lThreshold,uThreshold,category,isCount,batchLine);
      System.out.println("category -> "+category);
      System.out.println("Founded sources -> "+countDocs);
      System.out.println("Upper threshold -> "+uThreshold +"\nLower threshold -> "+lThreshold);
      System.out.println("------------------------------------------------\n");
      totalCount += countDocs;
    }
    System.out.println("Total sources found -> " +totalCount);
  }
  

  public static int readResultClassifier(File csvFile, KeywordExtractor kex, float lowThreshold,float upperThreshold,String category,boolean isCount,int batchLine) throws Exception{
    Map<String, List<String>> dataMap = read(csvFile.getAbsolutePath());
  
    List<String> ids = new ArrayList<>();
    ids.addAll(dataMap.keySet());
    SolrClient solr = new SolrClient();
    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationTestFolder+category+"_"+csvFile.getName().replace(".csv", "")+".txt"));
    int count = 0;
    String idToInsert= "";
    List<String> idList  = new ArrayList<>();
    for(String id: dataMap.keySet()){
      float probs = 0;
      try{
        probs = Float.parseFloat(dataMap.get(id).get(0));
      }catch (Exception e) {
        e.printStackTrace();
        continue;
      }
      if((probs <= upperThreshold) && (probs >= lowThreshold) ){       
        if(dataMap.get(id).get(1).replace("_", " ").contains(category)|| category.equals("")){
          if(count < batchLine || isCount){
            count++;
            p.println(id+" 1");
            p.flush();
            continue;
          }
          idList.add(id);
          count++;
        }
      }   
    }
    System.out.println(count+" - "+category);
    List<Source> sources = solr.getSourcesFromSolr(idList, Paper.class);
    int localcount = 0;
    for(Source s: sources){
        idToInsert += s.getId()+" 1\n";
        p.println(s.getId()+" - "+dataMap.get(s.getId()).get(0)+" - "+dataMap.get(s.getId()).get(1));
        
        List<String> tmp = new ArrayList<String>();
        String strTmp = "";
        for(String str: s.getTexts()){
          strTmp += str;
        }
        tmp.add(strTmp);
        
        p.println(kex.extractKeywordsFromTexts(tmp, numKey).stream().filter(l->l != null).flatMap(l->l.stream()).map(Keyword::getText).collect(Collectors.toList())+"\n");
        localcount ++;
        System.out.println(localcount+" - "+category);
        p.println(s.getTitle());
        p.println(s.getTexts().get(1));
        p.println("-------------------------------------\n");      
    }
  
    p.println("\n"+idToInsert);
    p.flush();
    p.close();
    return count;
  }

  public static void createTxtTrainingFromCsvResults(String csvFile,float lowThreshold,float upperThreshold,String PathWhereSave,String categoryFilter) throws FileNotFoundException{
    HashMap<String, List<String>> categoryMap = getCategoryMap(csvFile, lowThreshold, upperThreshold);

    PrintWriter p = new PrintWriter(new File(PathWhereSave.replace(".txt", categoryFilter+".txt")));
    if(categoryFilter.equals("")){
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
    }
    else{
      int countSource = 0;
      for(String id: categoryMap.get(categoryFilter)){ 
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

  public static HashMap<String, List<String>> getCategoryMap(String csvFile, float lowThreshold,float upperThreshold){
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
    return categoryMap;
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
          s.setKeywordList((ArrayList<Keyword>) kex.extractKeywordsFromTexts(texts, 20).stream().flatMap(l->l.stream()).collect(Collectors.toList()));
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
    DatasetBuilder.saveSources(listSource, PathConfigurator.trainingAndTestFolder+"TestGeneric.json");
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

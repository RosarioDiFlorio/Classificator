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

import eu.innovation.engineering.LSA.keywordExtractor.LSACosineKeywordExtraction;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.util.preprocessing.Paper;
import eu.innovation.engineering.util.preprocessing.SolrClient;
import eu.innovation.engineering.util.preprocessing.Source;

/**
 * @author Rosario
 *
 */
public class CSVDataReader {
  private static final int numKey = 10;
  private static final int limitSource = 500;
  private static String fileTestJson;
  private static boolean fromJson;
  private static boolean withKeyword;
  private static float upperThreshold;
  private static float lowThreshold;
  private static String workingPath;
  private static String categoryFolder;
  private static String csvPath;



  public static void main(String[] args) throws Exception{
    mainToTest(args);
    //mainToCreateDataset(args);
  }


  public static void mainToCreateDataset(String[] args) throws IOException{
    float upperThreshold = (float) 1.0;
    float lowThreshold = (float) 0.9;
    String  categoryFilter = "science";
    String fileCsv = PathConfigurator.applicationResultFolder+"resultsFullWiki.csv";
    String pathWhereSave = PathConfigurator.rootFolder+"testScience_FullWiki.txt";
    createDocumentSetFromCsvResults(fileCsv,lowThreshold,upperThreshold,pathWhereSave, categoryFilter);
    TxtDataReader txtReader = new TxtDataReader();
    //txtReader.mergeTxtDataset(oldDataset, pathWhereSave, 70, PathConfigurator.applicationFileFolder+"trainingDatasetMerged.txt");
  }

  /**
   * This function is the main function to read the results and analize them
   * @param args
   * @throws Exception
   */
  public static void mainToTest(String[] args) throws Exception{

    setCsvPath(PathConfigurator.applicationResultFolder+"resultsSubChemistry2.csv");
    setCategoryFolder("chemistry biology");
    setFromJson(false);
    setFileTestJson(getWorkingPath()+"/test.json");   
    setUpperThreshold((float) 1.0);
    setLowThreshold((float) 0.7);
    setWithKeyword(false);

    String batchCategory = "";
    String categoryFilter = "science";
    int batchLine = 0;   
    boolean isCount =  false;   
    boolean all = true;



    KeywordExtractor kex = null;
    if(!isCount && !isFromJson() || withKeyword){
      if(categoryFolder.equals(""))
        kex = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
      else
        kex = new LSACosineKeywordExtraction(PathConfigurator.keywordExtractorsFolder,getWorkingPath()+"/glossaries.json");
    }
    File f = new File(csvPath);
    if(f.isDirectory()){
      System.out.println("FOLDER -> "+csvPath);
      File[] list = f.listFiles();
      if(list.length>0){
        for(int i = 0; i<list.length;i++){
          int totalCount = 0;
          if(list[i].isFile() && list[i].getName().contains(".csv")){
            File fileToAnalyze = list[i];
            formatResultsFile(fileToAnalyze, kex,categoryFilter, batchCategory, isCount, batchLine, all);
          }
          System.out.println("-----------------------------------------------------\n");
        }
      }
    }else if(f.isFile()&&f.getName().contains(".csv")){
      formatResultsFile(f, kex, categoryFilter, batchCategory, isCount, batchLine, all);
    }
  }

  /**
   * 
   * This function call the function read ResultClassifier and format
   * the results for different setup and different type of keyword extraction.
   * to analyze in the deep the results.
   * @param fileToAnalyze
   * @param kex
   * @param categoryFilter
   * @param batchCategory
   * @param categoryFolder
   * @param isCount
   * @param batchLine
   * @param all
   * @throws Exception
   */
  public static void formatResultsFile(File fileToAnalyze, KeywordExtractor kex, String categoryFilter,String batchCategory,boolean isCount,int batchLine,boolean all) throws Exception{
    int totalCount = 0;
    System.out.println("FILE -> "+fileToAnalyze);
    if(all){
      List<String> categories = Collections.EMPTY_LIST;
      categories = TxtDataReader.getCategories(getWorkingPath()+"categories.txt");
      System.out.println(categories);
      for(int j = 0; j<categories.size();j++){
        int countDocs = 0;
        categoryFilter = categories.get(j);
        if(categoryFilter.equals(batchCategory) || batchCategory.equals("")){
          countDocs = readResultClassifier(fileToAnalyze,kex,categoryFilter,isCount,batchLine);
          System.out.println("category -> "+categoryFilter);
          System.out.println("Founded sources -> "+countDocs);
          System.out.println("Upper threshold -> "+upperThreshold +"\nLower threshold -> "+lowThreshold);
          System.out.println("------------------------------------------------\n");
          totalCount += countDocs;
          batchCategory = "";
          batchLine = 0;
        }
      }
    }else{
      int countDocs = readResultClassifier(fileToAnalyze,kex,categoryFilter,isCount,batchLine);
      System.out.println("category -> "+categoryFilter);
      System.out.println("Founded sources -> "+countDocs);
      System.out.println("Upper threshold -> "+upperThreshold +"\nLower threshold -> "+lowThreshold);
      System.out.println("------------------------------------------------\n");
      totalCount += countDocs;
    }
    System.out.println("Total sources found -> " +totalCount);
  }

  public static boolean isNumeric(String s) {  
    return s.matches("[-+]?\\d*\\.?\\d+");  
  } 

  public static int readResultClassifier(File csvFile, KeywordExtractor kex, String category,boolean isCount,int batchLine) throws Exception{
    Map<String, List<String>> dataMap = read(csvFile.getAbsolutePath());

    List<String> ids = new ArrayList<>();
    ids.addAll(dataMap.keySet());
    PrintWriter p = new PrintWriter(new File(PathConfigurator.applicationTestFolder+category+"_"+csvFile.getName().replace(".csv", "")+".txt"));
    int count = 0;
    String idToInsert= "";
    List<String> idList  = new ArrayList<>();
    for(String id: dataMap.keySet()){
      float probs = 0;
      try{
        if(isNumeric(dataMap.get(id).get(0)))
          probs = Float.parseFloat(dataMap.get(id).get(0));
      }catch (Exception e) {
        e.printStackTrace();
        continue;
      }
      if((probs <= upperThreshold) && (probs >= lowThreshold) ){       
        if(dataMap.get(id).get(1).replace("_", " ").replace(".", " ").equals(category)|| category.equals("")){
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

    if(!isFromJson() && withKeyword)
      System.out.println(count+" - "+category);
    List<Source> sources  = new ArrayList<>();
    if(fromJson){
      DatasetBuilder dataBuilder = new DatasetBuilder();
      Map<String, Source> mapSources = dataBuilder.loadMapSources(getFileTestJson());
      for(String id: idList){
        if(mapSources.containsKey(id))
          sources.add(mapSources.get(id));
      }
    } 
    else
      sources = SolrClient.getSourcesFromSolr(idList, Paper.class);


    int localcount = 0;
    for(Source s: sources){

      idToInsert += s.getId()+" 1\n";
      p.println(s.getId()+" - "+dataMap.get(s.getId()).get(0)+" - "+dataMap.get(s.getId()).get(1));

      if(!fromJson){
        List<String> tmp = new ArrayList<String>();
        String strTmp = "";

        for(String str: s.getTexts()){
          strTmp += str+".\n";
        }

        //        strTmp = s.getDescription();
        tmp.add(strTmp);
        if(withKeyword)
          p.println(kex.extractKeywordsFromTexts(tmp, numKey).stream().filter(l->l != null).flatMap(l->l.stream()).map(Keyword::getText).collect(Collectors.toList())+"\n");
      }
      else
        if(withKeyword)
          p.println(s.getKeywordList().stream().map(Keyword::getText).collect(Collectors.toList()));

      localcount ++;
      if(!isFromJson() && withKeyword)
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

  public static void createDocumentSetFromCsvResults(String csvFile,float lowThreshold,float upperThreshold,String PathWhereSave,String categoryFilter) throws FileNotFoundException{
    HashMap<String, List<String>> categoryMap = getCategoryMap(csvFile, lowThreshold, upperThreshold);

    PrintWriter p = new PrintWriter(new File(PathWhereSave.replace(".txt", categoryFilter+".txt")));
    if(categoryFilter.equals("")){
      for(String category: categoryMap.keySet()){
        int countSource = 0;
        p.println(category.replace("_", " ").replace(".", " "));
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

  /**
   * The basic function that read the the csv file.
   * @param csvFile
   * @return
   */
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


  public static String getCategoryFolder() {
    return categoryFolder;
  }

  public static void setCategoryFolder(String categoryFolder) {
    CSVDataReader.categoryFolder = categoryFolder;
    if(categoryFolder.length()<1)
      setWorkingPath(PathConfigurator.rootFolder);
    else
      setWorkingPath(PathConfigurator.rootFolder+categoryFolder+"/");
  }

  public static String getFileTestJson() {
    return fileTestJson;
  }

  public static void setFileTestJson(String fileTestJson) {
    CSVDataReader.fileTestJson = fileTestJson;
  }

  public static boolean isFromJson() {
    return fromJson;
  }

  public static void setFromJson(boolean fromJson) {
    CSVDataReader.fromJson = fromJson;
  }


  public static float getUpperThreshold() {
    return upperThreshold;
  }

  public static void setUpperThreshold(float upperThreshold) {
    CSVDataReader.upperThreshold = upperThreshold;
  }

  public static float getLowThreshold() {
    return lowThreshold;
  }

  public static void setLowThreshold(float lowThreshold) {
    CSVDataReader.lowThreshold = lowThreshold;
  }

  public static String getWorkingPath() {
    return workingPath;
  }

  public static void setWorkingPath(String workingPath) {
    CSVDataReader.workingPath = workingPath;
  }

  public static String getCsvPath() {
    return csvPath;
  }

  public static void setCsvPath(String csvPath) {
    CSVDataReader.csvPath = csvPath;
  }


  public static boolean isWithKeyword() {
    return withKeyword;
  }


  public static void setWithKeyword(boolean withKeyword) {
    CSVDataReader.withKeyword = withKeyword;
  }



}

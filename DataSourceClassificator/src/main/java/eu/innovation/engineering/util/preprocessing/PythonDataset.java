package eu.innovation.engineering.util.preprocessing;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;

public class PythonDataset {


  public static void main(String[] args) throws IOException{

    String pathSources = PathConfigurator.datasetFolder+"TestChemistry_FullWiki.txt";
    String pathCategories = PathConfigurator.rootFolder+"science/"+"categories.txt";
    fromTxtFile(PathConfigurator.pyFolder+"testChemistry_fullwiki",pathSources,pathCategories);

    String dataseFolderPath = PathConfigurator.pyFolder+"test_Solr_10k";
    String jsonFile = PathConfigurator.pyFolder+"test.json";
    int limitTexts = 10000;
    boolean multiLabeled = false;
    //fromJsonFile(dataseFolderPath, jsonFile, pathCategories, limitTexts, multiLabeled);

  }

  public static void fromTxtFile(String folderName,String sourcesFile,String categoryFiles) throws IOException{
    TxtDataReader txtDataReader = new TxtDataReader();
    Map<String, HashMap<String, String>> categoryMap = txtDataReader.categoriesWithIds(sourcesFile, categoryFiles);

    boolean success = new File(folderName).mkdir();  
    for(String category: categoryMap.keySet()){
      String folderCategory = folderName+"/"+category.replace(" ", ".");
      success = new File(folderCategory).mkdir();
      HashMap<String, String> sourcesMap = categoryMap.get(category);

      List<String> idSources = sourcesMap.keySet().stream().collect(Collectors.toList());
      List<Source> sources = SolrClient.getSourcesFromSolr(idSources, Paper.class);
      for(Source s: sources){
        PrintWriter p = new PrintWriter(new File(folderCategory+"/"+s.getId()));
        p.println(s.getTitle());
        p.println(s.getDescription());
        p.flush();
        p.close();
      } 
    }

  }

  public static void fromJsonFile(String folderName,String sourcesFile,String categoryFiles,int limitTexts,boolean multiLabeled) throws IOException{
    List<String> categories = TxtDataReader.getCategories(categoryFiles);
    List<Source> sources = DatasetBuilder.loadSources(sourcesFile);
    System.out.println("Sources avaiable -> "+sources.size());

    boolean success = new File(folderName).mkdir();
    for(String category : categories){
      String subFolderPath = folderName+"/"+category.replace(" ",".");
      success = new File(subFolderPath).mkdir();
    }

    int count = 0;
    for(Source s: sources){
      if(count >= limitTexts)
        break;
      String subFolderPath = "";
      if(multiLabeled)
        subFolderPath = folderName+"/"+s.getCategoryList().get(0).getLabel().replace(" ",".");
      else
        subFolderPath = folderName+"/"+categories.get(0).replace(" ", ".");
      PrintWriter p = new PrintWriter(new File(subFolderPath+"/"+s.getId()));
      p.println(s.getTitle());
      p.println(s.getDescription());
      p.flush();
      p.close();
      count++;
    }   
  }



}

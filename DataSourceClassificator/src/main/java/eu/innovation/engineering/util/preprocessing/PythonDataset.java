package eu.innovation.engineering.util.preprocessing;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;

public class PythonDataset {

  
  public static void main(String[] args) throws IOException{
    
    //String pathSources = PathConfigurator.rootFolder+"outputResultsRoot.txt";

    String pathSources = PathConfigurator.rootFolder+"trainingResultsBig.txt";
    String pathCategories = PathConfigurator.rootFolder+"categories.txt";

    createDataset("bigTrainDataset",pathSources,pathCategories);
  }
  
  public static void createDataset(String folderName,String sourcesFile,String categoryFiles) throws IOException{
 
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
  
  
  
}

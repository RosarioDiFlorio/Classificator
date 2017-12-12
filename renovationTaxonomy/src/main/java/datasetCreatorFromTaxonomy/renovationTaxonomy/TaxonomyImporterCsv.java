package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxonomyImporterCsv {

  public static void main(String[] args) throws FileNotFoundException, IOException{
    String csvFile ="";
    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<String>> dataMap = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] csvData = line.split(cvsSplitBy); 
        List<String> data = new ArrayList<>();
        if(csvData.length>=2){
          for(int i =0;i<csvData.length-1;i++){
            data.add(csvData[i].trim());
          }
          String key = csvData[csvData.length-1].trim().replace("en.wikipedia.org/wiki/", "");
          if(dataMap.containsKey(key))
            System.out.println(key);
          if(!key.equals(""))
            dataMap.put(key, data);
        }
      }
    }
  }
}

package eu.innovation.engineering.prepocessing.featurextractor;


import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.util.preprocessing.Source;



/**
 * Crea le matrici doc-keywords e la trasposta keywords-doc, partendo dal dataset dataset/documentWithKeywords.txt
 *
 */
public class CreateMatrix 
{
  
  

    public static void main( String[] args ) throws IOException
    {
      DatasetBuilder pb = new DatasetBuilder();
      pb.parseDatasetFromJson("dataset/datasetWithKeywords.json");
      
      ArrayList<Source> paperList = pb.getSourceList();
      HashSet<String> keywordList = new HashSet<String>();
      
      for(Source p : paperList){
        for(Keyword k : p.getKeywordList()){
          keywordList.add(k.getText());
        }
      }
      
      //System.out.println(returnMatrixDocumentsKeywords(paperList, keywordList));
      
      HashMap<String, ArrayList<Double>> result = getMatrixDocumentsKeywords(paperList, keywordList);
      
      int count = 0;
      int countNum=0;
      for(String key: result.keySet()){
        if(count < 1){
           for(Double d : result.get(key)){
             if(d>0){
               countNum++;
             }
           }
          count++;
        }
        else{
          break;
        }
      }
      
      System.out.println(countNum);
      
    }
    
    
    
    

    /**
     * 
     * @param paperList: document list
     * @param featureList:  keywrods as features
     * @return matrix 
     * @throws IOException 
     */
    public static HashMap<String, ArrayList<Double>> getMatrixDocumentsKeywords (ArrayList<Source> paperList, HashSet<String> featureList) throws IOException{
      HashMap<String, ArrayList<Double>> matrixDocumentsKeywords = new HashMap<String, ArrayList<Double>>();
      ObjectMapper mapper = new ObjectMapper();
      FileWriter writer = new FileWriter("matrixDocumentsKeywords.json");
      JsonGenerator g = mapper.getFactory().createGenerator(writer);
      for(Source p: paperList){
        ArrayList<Double> features = new ArrayList<Double>();
        for(String f : featureList){
          boolean contains = false;
          for(Keyword k : p.getKeywordList()){
            if(k.getText().equals(f)){
              //System.out.println(k.getText());
              contains = true;
              features.add(k.getRelevance());
            }
            
          }
          if(!contains){
            features.add(0.0);
          }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(g, features);
         matrixDocumentsKeywords.put(p.getId(), features);
      }
      
      return matrixDocumentsKeywords;
    }
    
    
    
    /**
     * 
     * @param paperList: document list
     * @param featureList: keywrods as items
     * @return
     * @throws IOException 
     */
    public static HashMap<String, ArrayList<Double>> getMatrixKeywordsDocuments (ArrayList<Source> paperList, HashSet<String> itemList) throws IOException{
      HashMap<String, ArrayList<Double>> matrixKeywordsDocuments = new HashMap<String, ArrayList<Double>>();
      ObjectMapper mapper = new ObjectMapper();
      FileWriter writer = new FileWriter("matrixKeywordsDocuments.json");
      JsonGenerator g = mapper.getFactory().createGenerator(writer);
      for(String item : itemList){
        ArrayList<Double> features = new ArrayList<Double>();
        for(Source p: paperList){
          boolean contains = false;
          for(Keyword k : p.getKeywordList()){
            if(k.getText().equals(item)){
              contains = true;
              features.add(k.getRelevance());
              break;
            }
          }
          if(!contains){
            features.add(0.0);
          }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(g, features);
        matrixKeywordsDocuments.put(item, features);
      }
      return matrixKeywordsDocuments;
    }
    
}

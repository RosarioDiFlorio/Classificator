package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.datareader.TxtDataReader;
import eu.innovation.engineering.prepocessing.featurextractor.CategoryVector;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.featurextractor.Item;
import eu.innovation.engineering.util.featurextractor.ItemWrapper;
import eu.innovation.engineering.util.featurextractor.SourceVector;
import eu.innovation.engineering.util.preprocessing.Source;

/**
 * Questa classe serve per generare il trainingSet per le categorie indicate
 * Usa : 
 *  - un file Json contenente i datasource (con keywords)
 *  - un file txt che contiene le categorie
 *  - il path della folder che contiene i dizionari delle categorie (file txt)
 * @author lomasto
 *
 */

public class TrainingSetBuilder {

  private static final String category = "science";

  public static void main(String[] args) throws IOException { 
    String path = PathConfigurator.rootFolder + category;
    if(!category.equals(""))
      path = PathConfigurator.rootFolder + category +"/";



    CategoryVector categoryVector = new CategoryVector();
    CategoryVector.execute(path+"categories.txt",null,path);

    clusterSubCategory(PathConfigurator.applicationFileFolder+"sourceVectors.json",path+"vectorCategory.json", "science", path);

  }

  public static void clusterSubCategory(String sourceFile, String categoryFile, String categoryChoose, String path) throws JsonParseException, JsonMappingException, IOException{

    ObjectMapper mapper = new ObjectMapper();
    HashMap<String,float[]> categoryVectorList = mapper.readValue(new File(categoryFile), new TypeReference<HashMap<String,float[]>>() {});

    List<SourceVector> sourceList = SourceVectorBuilder.loadSourceVectorList(sourceFile);

    PrintWriter writer = new PrintWriter(PathConfigurator.applicationFileFolder+"CosineSimilarityResults.txt");
    ArrayList<Item> items = new ArrayList<Item>(); 

    for(SourceVector source : sourceList){
      if(source.getCategory().contains(categoryChoose)){
        writer.println("\nID: "+source.getId());
        writer.println("TITLE: "+source.getTitle());
        writer.println("KEYWORDS: "+source.getKeywords().toString());
        Item item = new Item();
        item.setId(source.getId());
        item.setDatasource("Paper");
        item.setTitle(source.getTitle()+"\n"+source.getKeywords().toString()+"\n");
        double[] features = new double[categoryVectorList.size()];
        int count = 0;
        String hightCategory = "";
        double valHightCategory = 0;
        for(String category : categoryVectorList.keySet()){

          features[count] = FeatureExtractor.cosineSimilarity(source.getVector(), categoryVectorList.get(category));
          writer.println("      "+category+": "+features[count]);
          if(features[count]>valHightCategory){
            valHightCategory = features[count];
            hightCategory = category;
          }
          count++;
        }
        item.setBestFeature(hightCategory);
        item.setFeatures(features);
        items.add(item);
      }

    }
    writer.flush();
    writer.close();




    List<ItemWrapper> clusterInput = items.stream().map(ItemWrapper::new).collect(Collectors.toList());
    KMeansPlusPlusClusterer<ItemWrapper> clusterer = new KMeansPlusPlusClusterer<ItemWrapper>(4);

    System.out.println("Number datasource to create dictionaries: "+clusterInput.size()+" num Cluster:"+4);
    System.out.println("Starting k-means");
    List<CentroidCluster<ItemWrapper>> clusterResults = clusterer.cluster(clusterInput);
    System.out.println("Ended k-means");

    System.out.println("DaviesBouldin-Index: "+ClusteringKMeans.DaviesBouldinIndex(clusterResults,4));

    writer = new PrintWriter(PathConfigurator.applicationFileFolder+"clusters.txt");
    for (int i=0; i<clusterResults.size(); i++) {
      writer.println("\nCluster: "+i);
      System.out.println("\n\nCluster: "+i);
      for (ItemWrapper itemWrapper : clusterResults.get(i).getPoints()){
        writer.println("    id: "+itemWrapper.getItem().getId()+"   Keywords: "+itemWrapper.getItem().getTitle());
        System.out.println("    id: "+itemWrapper.getItem().getId()+"   Keywords: "+itemWrapper.getItem().getTitle());
      }
    }
    writer.flush();
    writer.close();


    List<Source> source = DatasetBuilder.loadSources(PathConfigurator.applicationFileFolder+"sources.json");

    List<Source> newSource = new ArrayList<Source>();
    for(Item item : items){
      for(Source s : source){
        if(s.getId().equals(item.getId())){
          if(s.getCategoryList().get(0).getLabel().contains(categoryChoose)){
            CategoriesResult category = new CategoriesResult();
            category.setLabel(item.getBestFeature());
            category.setScore(1.0);
            ArrayList<CategoriesResult> categoryList = new ArrayList<CategoriesResult>();
            categoryList.add(category);
            s.setCategoryList(categoryList);
          }
          newSource.add(s);
        }
      }
    }




     
    List<String> listCategories = TxtDataReader.getCategories(path+"categories.txt");
    newSource=keywordFilterLSA(newSource, listCategories);
    
    DatasetBuilder.saveSources(newSource, path+"/training.json");


  }


  public static List<Source> keywordFilterLSA (List<Source> sourceList, List<String> categoryChoose) throws IOException{


    for(Source src : sourceList ){
      ArrayList<Keyword> keywordList = src.getKeywordList();
      for(Iterator<Keyword> it = keywordList.iterator(); it.hasNext();){
        Keyword k = it.next();
        double max = 0;
        for(String category : categoryChoose){

          ArrayList<List<String>> textList = new ArrayList<List<String>>();
          List<String> list = new ArrayList<String>();
          if(category.contains(" ")){
            String[] categories = category.split(" ");
            for(String s : categories){
              list.add(s);
            }


          }
          else{
            list.add(category);
          }

          textList.add(list);
          float[] vectorCategoryChoose = ClusteringKMeans.returnVectorsFromTextList(textList)[0];
          String label = k.getText();
          textList = new ArrayList<List<String>>();
          list = new ArrayList<String>();
          list.add(label);
          textList.add(list);
          float[] vector = ClusteringKMeans.returnVectorsFromTextList(textList)[0];
          double vectorResult = FeatureExtractor.cosineSimilarity(vectorCategoryChoose, vector);
          
          if(vectorResult>max){
            max=vectorResult;
          }
          
          //System.out.println(label+" "+vectorResult);

        }
        
        if(max<0.1){
          System.out.println("Rimossa. "+k.getText()+" "+max);
          it.remove();
        }

      }

    }
    return sourceList;

  }







}

package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.featurextractor.CategoryVector;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.featurextractor.Item;
import eu.innovation.engineering.util.featurextractor.ItemWrapper;
import eu.innovation.engineering.util.featurextractor.SourceVector;

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

  
  
  
  public static void main(String[] args) throws IOException {
    CategoryVector categoryVector = new CategoryVector();
    CategoryVector.execute(PathConfigurator.categories+"science.txt",null);
    clusterSubCategory(PathConfigurator.applicationFileFolder+"sourceVectors.json",PathConfigurator.categories+"scienceJson.json", "science");

  }
  
  public static void clusterSubCategory(String sourceFile, String categoryFile, String categoryChoose) throws JsonParseException, JsonMappingException, IOException{

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
        for(String category : categoryVectorList.keySet()){
          
          features[count] = FeatureExtractor.cosineSimilarity(source.getVector(), categoryVectorList.get(category));
          writer.println("      "+category+": "+features[count]);
          count++;
        }
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
  }
    
}

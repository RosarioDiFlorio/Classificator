package eu.innovation.engineering.prepocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.featurextractor.CategoryVector;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.featurextractor.Item;
import eu.innovation.engineering.util.featurextractor.SourceVector;
import eu.innovation.engineering.util.preprocessing.Source;
import eu.innovation.engineering.util.preprocessing.TextValidator;

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

  public static void main(String[] args) throws Exception { 
    String path = PathConfigurator.rootFolder + category;
    if(!category.equals(""))
      path = PathConfigurator.rootFolder + category +"/";
    
    boolean fromSolr = true;
    boolean withCategories = false;
    
    SourceVectorBuilder sourceVectorBuilder = new SourceVectorBuilder();
    //    sourceVectorBuilder.buildSourceVectors(PathConfigurator.rootFolder+"science/","science",withCategories,fromSolr);
    sourceVectorBuilder.buildSourceVectors(path,category,withCategories,fromSolr);

    // SERVE PER GENERARE IL FILE CHE CONTIENE I VETTORI DELLE CATEGORIE
    CategoryVector.execute(path+"categories.txt",null,path);
    // SERVE PER GENEARRE IL TRAINING.JSON CON LE SOTTOCATEGORIE DELLA CATEGORIA SCELTA
    clusterSubCategory(path+"sourceVectors.json",path+"vectorCategory.json", category, path);

  }



  public static void clusterSubCategory(String sourceFile, String categoryFile, String categoryChoose, String path) throws JsonParseException, JsonMappingException, IOException{

    ObjectMapper mapper = new ObjectMapper();
    HashMap<String,float[]> categoryVectorList = mapper.readValue(new File(categoryFile), new TypeReference<HashMap<String,float[]>>() {});

    //carico una lista di sourceVector, ovvero delle source che contengono anche il vettore (che rappresenta la concatenazione delle keywords)
    List<SourceVector> sourceList = SourceVectorBuilder.loadSourceVectorList(sourceFile);



    //creo una lista di item, per ogni source(item) mi calcolo la subcategory migliore 
    ArrayList<Item> items = new ArrayList<Item>(); 


    for(SourceVector source : sourceList){
      if(source.getCategory().contains(categoryChoose)){

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



    //carcico il file che contiene i source con la cagetory scelta e tutte le info (title, id, keywords) faccio un merge con la lista di Item per cambiare la categoria (da category a subcategory)
    List<Source> source = DatasetBuilder.loadSources(path+"sources.json");

    TextValidator textValidatorForDescription = new TextValidator(Configurator.minDescriptionLength);

    List<Source> newSource = new ArrayList<Source>();
    for(Item item : items){
      for(Source s : source){
        if(s.getId().equals(item.getId()) && textValidatorForDescription.analyzer(s.getDescription())){
          CategoriesResult category = new CategoriesResult();
          category.setLabel(item.getBestFeature());
          category.setScore(1.0);
          ArrayList<CategoriesResult> categoryList = new ArrayList<CategoriesResult>();
          categoryList.add(category);
          s.setCategoryList(categoryList);
          newSource.add(s);
        }
        else{
          if(!textValidatorForDescription.analyzer(s.getDescription()) && s.getId().equals(item.getId()))
            System.out.println("PAPER ELIMINATO: "+s.getDescription()+"\n");
          }
      }
    }


    //List<String> listCategories = TxtDataReader.getCategories(path+"categories.txt");
    //newSource=keywordFilterLSA(newSource, listCategories);

    DatasetBuilder.saveSources(newSource, path+"training.json");


  }


  //Metodo che filtra le keywords delle source. Da usare solo quando si usa InnenExtractor nelle sottogatergorie. Permette di eliminare le keywords con basso valore semantico rispetto alla subcategory
  public static List<Source> keywordFilterLSA (List<Source> sourceList, List<String> categoriesChoose) throws IOException{


    for(Source src : sourceList ){
      ArrayList<Keyword> keywordList = src.getKeywordList();
      for(Iterator<Keyword> it = keywordList.iterator(); it.hasNext();){
        Keyword k = it.next();
        double max = 0;
        for(String category : categoriesChoose){

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
          ClusteringKMeans clustering = new ClusteringKMeans();
          float[] vectorCategoryChoose = clustering.returnVectorsFromTextList(textList)[0];
          String label = k.getText();
          textList = new ArrayList<List<String>>();
          list = new ArrayList<String>();
          list.add(label);
          textList.add(list);

          float[] vector = clustering.returnVectorsFromTextList(textList)[0];
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

package eu.innovation.engineering.prepocessing.featurextractor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.util.featurextractor.Item;
import eu.innovation.engineering.util.featurextractor.ItemWrapper;
import eu.innovation.engineering.util.preprocessing.Source;
import eu.innovationengineering.word2vec.common.Constants;
import eu.innovationengineering.word2vec.common.request.bean.VectorListRequestBean;
import eu.innovationengineering.word2vec.service.rest.impl.Word2vecServiceImpl;


public class ClusteringKMeans {

  public static void main (String args[]) throws IOException{

    //clusterWithDatasourceAsItems();
  }




  public static void clusterWithKeywordsAsItems() throws IOException {
    CreateMatrix matrixCreator = new CreateMatrix();

    DatasetBuilder pb = new DatasetBuilder();
    pb.parseDatasetFromJson("dataset/datasetWithKeywords.json");

    ArrayList<Source> paperList = pb.getSourceList();
    HashSet<String> keywordList = pb.returnAllKeywords(paperList);

    HashMap<String, ArrayList<Double>> matrixKeywordsDocument = matrixCreator.getMatrixKeywordsDocuments(paperList,keywordList);

    ArrayList<Item> items = new ArrayList<Item>();



    for(String k : keywordList){
      Item item = new Item();
      item.setId(k);
      item.setFeatures(matrixKeywordsDocument.get(k).stream().mapToDouble(Double::doubleValue).toArray());   
      items.add(item);
    }


    // we have a list of our locations we want to cluster. create a     
    List<ItemWrapper> clusterInput = new ArrayList<ItemWrapper>(items.size());

    for(Item i : items){
      clusterInput.add(new ItemWrapper(i));
    }

    // lo metto a null per liberare memoria
    items=null;



    // initialize a new clustering algorithm. 
    // we use KMeans++ with 10 clusters and 10000 iterations maximum.
    // we did not specify a distance measure; the default (euclidean distance) is used.
    KMeansPlusPlusClusterer<ItemWrapper> clusterer = new KMeansPlusPlusClusterer<ItemWrapper>(23, clusterInput.size());

    List<CentroidCluster<ItemWrapper>> clusterResults = clusterer.cluster(clusterInput);




    FileWriter write = new FileWriter("Clusters.txt");
    // output the clusters
    for (int i=0; i<clusterResults.size(); i++) {
      System.out.println("Cluster " + i);
      write.write("Cluster " + i+"\n");
      for (ItemWrapper itemWrapper : clusterResults.get(i).getPoints()){
        System.out.println(itemWrapper.getItem().getId());
        write.write(itemWrapper.getItem().getId()+"\n");
      }
      write.write("\n\n");

    }

    write.flush();
    write.close();



  }



  public HashMap<String, Dictionary> clusterWithDatasourceAsItems() throws IOException {
    CreateMatrix matrixCreator = new CreateMatrix();
    
    
    DatasetBuilder pb = new DatasetBuilder();
    pb.parseDatasetFromJson(PathConfigurator.backUpDatasetFolder+"test_complete.json");
    // PRENDO LA LISTA DI PAPER DAL FILE USANDO IL METODO DELL OGGETTO pb
    ArrayList<Source> paperList = pb.getSourceList();
   
    //INIZIALIZZO UNA LISTA DI ITEMS, CHE SARANNO GLI OGGETTI CHE VERRANNO CLUSTERIZZATI
    ArrayList<Item> items = new ArrayList<Item>();

    //CI PRENDIAMO I VETTORI PER OGNI PAPER
    float resultsVector[][] = returnVectorFromFeatures(paperList);

    ToDoubleFunction<float[]> norm = v -> {
      double normSquared = 0.0;
      for (int i = 0; i < v.length; i++) {
        normSquared += v[i] * v[i];
      }
      return Math.sqrt(normSquared);
    };

    for(int i=0; i<resultsVector.length;i++){
      if (norm.applyAsDouble(resultsVector[i]) == 0.0) {
        System.out.println("Skipping zero-length vector for id " + paperList.get(i).getId());
        continue;
      }
      Item item = new Item();
      item.setId(paperList.get(i).getId());

      double results[] = new double[resultsVector[i].length];
      for (int j = 0; j < resultsVector[i].length; j++) {
        results[j] = resultsVector[i][j];
      }
      //AGGIUNGO ALL'ITEM IL VETTORE COME FEATURE 
      item.setFeatures(results);
      //item.setFeatures(matrixDocuementKeywords.get(p.getId()).stream().mapToDouble(Double::doubleValue).toArray());   
      item.setDatasource("Paper");
      items.add(item);
    }


    // we have a list of our locations we want to cluster. create a     
    List<ItemWrapper> clusterInput = items.stream().map(ItemWrapper::new).collect(Collectors.toList());

    // initialize a new clustering algorithm. 
    // we use KMeans++ with 10 clusters and 10000 iterations maximum.
    // we did not specify a distance measure; the default (euclidean distance) is used.
    KMeansPlusPlusClusterer<ItemWrapper> clusterer = new KMeansPlusPlusClusterer<ItemWrapper>(Configurator.numFeatures, clusterInput.size());

    System.out.println("Number datasource to create dictionaries: "+clusterInput.size());
    System.out.println("Starting k-means");
    List<CentroidCluster<ItemWrapper>> clusterResults = clusterer.cluster(clusterInput);
    System.out.println("Ended k-means");

    //creo la lista di dizionari, ogni dizionario contiene la lista di keywords e il vettore che rappresenta l'intero dizionario, utile nell'analisi LSA
    HashMap<String,Dictionary> dictionaries = new HashMap<>();

    //ciclo sui cluster ottenuti, per ogni cluster creo un dizionario che contiene tutte le keywords dei paper che appartengono al cluster
    for (int i=0; i<clusterResults.size(); i++) {
      HashSet<String> keywords = new HashSet<>();
      for (ItemWrapper itemWrapper : clusterResults.get(i).getPoints()){
        String id = itemWrapper.getItem().getId();
        for(Source p : paperList){
          if(p.getId().equals(id))
            for(Keyword k : p.getKeywordList()){
              keywords.add(k.getText());
            }
        }
      }
      Dictionary dictionary= new Dictionary();
      dictionary.setKeywords(keywords);
      dictionaries.put("Cluster "+i, dictionary);
    }

    //STAMPO SU FILE I CLUSTER OTTENUTI
    FileWriter writer = new FileWriter(PathConfigurator.dictionariesFolder+"dictionaries.txt");

    for(String cluster : dictionaries.keySet()){
      writer.write(cluster+"\n");
      Dictionary currentDictionary = dictionaries.get(cluster);
      for(String keyword : currentDictionary.getKeywords()){
        writer.write("    "+keyword+"\n");
      }
      writer.write("\n\n");
    }

    writer.flush();
    writer.close();
    
    // per ogni dizionario calcolo anche i vettori che mi serviranno successivamente. 
    HashMap<String, Dictionary> finalDictionaries = returnVectorForDictionaries(dictionaries);

    
    return finalDictionaries;

  }




  private static HashMap<String, Dictionary> returnVectorForDictionaries(HashMap<String, Dictionary> dictionaries) throws IOException {

    List<List<String>> docsK = new ArrayList<List<String>>();

    // pr ogni dizionario
    for(String cluster: dictionaries.keySet()){
      // creo l'arrayList di stringhe da passare a word2Vec
      ArrayList<String> stringToVec = new ArrayList<>();
      for(String k : dictionaries.get(cluster).getKeywords()){
        String parts[] = k.split(" ");
        Arrays.stream(parts).forEach(stringToVec::add);
      }
      docsK.add(stringToVec);
    }
    VectorListRequestBean vectorListRequest = new VectorListRequestBean();
    vectorListRequest.setDocs(docsK);

    // faccio le richieste a word2Vec per i vettori
    WebClient webClient = WebClient.create("http://smartculture-projects.innovationengineering.eu/word2vec-rest-service/", Arrays.asList(new JacksonJaxbJsonProvider()));

    try (Word2vecServiceImpl word2vecService = new Word2vecServiceImpl()) {      
      word2vecService.setWebClient(webClient);
      float[][] vectorListKeyword = word2vecService.getVectorList(Constants.GENERAL_CORPUS, Constants.ENGLISH_LANG, vectorListRequest);
      int iteratorDictionary=0;
      for(String dictionary : dictionaries.keySet()){
        double[] app = new double[vectorListKeyword[iteratorDictionary].length];
        for(int i = 0; i<vectorListKeyword[iteratorDictionary].length;i++){
          app[i]= vectorListKeyword[iteratorDictionary][i];
        }
        // converto il vettore double in un vettore float
        float [] floatApp = new float[app.length];
        for(int i=0;i<app.length;i++){
          floatApp[i]=(float) app[i];
        }
        //aggiungo al dizionario il vettore ottenuto da word2vec
        dictionaries.get(dictionary).setVector(floatApp);
        iteratorDictionary++;
      }
      
    }
    
    return dictionaries;
    
  }




  public static float[][] returnVectorFromFeatures(ArrayList<Source> paperList) throws IOException {

    //ISTANZIAMO UNA MATRICE DI STRINGHE
    List<List<String>> docsK = new ArrayList<List<String>>();
    
    //PER OGNI PAPER COSTRUIAMO IL SET DI KEYWORDS DA CUI POI OTTENERE IL VETTORE
    for(Source p: paperList){
      //calcolo la relevance minima
      double minRelevance = p.getKeywordList().stream().mapToDouble(Keyword::getRelevance).min().getAsDouble();

      // creo l'arrayList di stringhe da passare a word2Vec
      ArrayList<String> stringToVec = new ArrayList<>();
      for(Keyword k : p.getKeywordList()){
        double resultDivision = k.getRelevance()/minRelevance;
        int numOccurence = (int) Math.ceil(resultDivision);
        //Aggiungo la keyword nel vettore di keywords "numOccurence" volte
        String parts[] = k.getText().split(" ");
        for(int i = 0; i<numOccurence; i++){
          Arrays.stream(parts).forEach(stringToVec::add);
        }
      }
      //AGGIUNGO LA LISTA DI KEYWORDS ALLA MATRICE PER IL PAPER CORRENTE
      docsK.add(stringToVec);
    }
    VectorListRequestBean vectorListRequest = new VectorListRequestBean();
    vectorListRequest.setDocs(docsK);

    //chiamo il wordToVec per calcolare il vettore delle stinghe ottenute

    WebClient webClient = WebClient.create("http://smartculture-projects.innovationengineering.eu/word2vec-rest-service/", Arrays.asList(new JacksonJaxbJsonProvider()));

    try (Word2vecServiceImpl word2vecService = new Word2vecServiceImpl()) {      
      word2vecService.setWebClient(webClient);
      float[][] vectorListKeyword = word2vecService.getVectorList(Constants.GENERAL_CORPUS, Constants.ENGLISH_LANG, vectorListRequest);
      int vectorLength = Arrays.stream(vectorListKeyword).mapToInt(v -> v.length).max().getAsInt();
      for (int i = 0; i < vectorListKeyword.length; i++) {
        if (vectorListKeyword[i].length != vectorLength) {
          float newVector[] = new float[vectorLength];
          System.arraycopy(vectorListKeyword[i], 0, newVector, 0, vectorListKeyword[i].length);
          vectorListKeyword[i] = newVector;
        }
      }
      return vectorListKeyword ;

    }


  }


}

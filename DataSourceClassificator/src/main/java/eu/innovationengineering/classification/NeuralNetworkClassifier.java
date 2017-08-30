package eu.innovationengineering.classification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.NeuronProperties;
import org.neuroph.util.TransferFunctionType;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.prepocessing.DatasetBuilder;
import eu.innovation.engineering.prepocessing.featurextractor.ClusteringKMeans;
import eu.innovation.engineering.prepocessing.featurextractor.Dictionary;
import eu.innovation.engineering.prepocessing.featurextractor.FeatureExtractor;
import eu.innovation.engineering.util.featurextractor.BestSetting;
import eu.innovation.engineering.util.featurextractor.Features;
import eu.innovation.engineering.util.featurextractor.IdAndTarget;
import eu.innovation.engineering.util.preprocessing.Source;



public class NeuralNetworkClassifier {

  private static int numClasses;
  
  public static void main(String args[]) throws IOException, InterruptedException{
    mainNN(args);
    
  }
  

  public static void mainNN(String args[]) throws IOException, InterruptedException{

    ClusteringKMeans clusteringDictionaries = new ClusteringKMeans();
    HashMap<String, Dictionary> dictionaries = clusteringDictionaries.clusterWithDatasourceAsItems();

    FeatureExtractor featureExtractor = new FeatureExtractor();
    HashSet<String> categories = (HashSet<String>) Configurator.getCategories();

    System.out.println("CATEGORIE");
    System.out.println(categories.toString());

    // PARTE DI CODICE CON IL BALANCER ED UN UNICO DATASET
    /*PaperBuilder pb = new PaperBuilder();
    pb.parseDatasetFromJson("datasetTrainingAndTest/datasetSupervisionato.json");
    ArrayList<Paper> listaPaper = pb.getListPapers();
    PaperBalancer balancer = new PaperBalancer();
    System.out.println("Starting balancer for training and test ");
    Bilancio le classi in modo da avere un trainingSet e un testSet bilanciato
    balancer.balancer(listaPaper,categories);
    System.out.println("End balancer for training and test");    
    ArrayList<Paper> trainingSet = balancer.getTrainingSet();
    ArrayList<Paper> testSet = balancer.getTestSet();*/
    ////////////////////////////////////////////////////


    //PARTE DI CODICE SENZA BALANCER E DUE DATASET DIVERSI
    DatasetBuilder pbTraining = new DatasetBuilder();
    pbTraining.parseDatasetFromJson("datasetTrainingAndTest/train.json");
    ArrayList<Source> trainingSet = pbTraining.getSourceList();



    //STAMPO IL DATASET DI TRAINING CON I PAPER DIVISI PER CLASSI, PER VEDERE COME E' BILANCIATO
    System.out.println("\n DATASET DI TRAINING \n");
    HashMap<String,ArrayList<Source>>trainingPapersForCategory=categoryListWithAssociatePapers(trainingSet,categories);
    for(String key: trainingPapersForCategory.keySet()){
      System.out.println(key+": "+trainingPapersForCategory.get(key).size());
    }

    //STAMPO SU FILE LE CLASSI CONTENENTI I PAPER DI TRAINING 
    FileWriter fileWriterpaperForCategory = new FileWriter("papersForCategory.txt"); 
    for(String key : trainingPapersForCategory.keySet()){
      fileWriterpaperForCategory.write("\n"+key.toUpperCase()+"\n");
      for(Source p : trainingPapersForCategory.get(key)){
        fileWriterpaperForCategory.write(p.getId()+"\n");
      }
    }

    fileWriterpaperForCategory.flush();
    fileWriterpaperForCategory.close();

    DatasetBuilder pbTesting= new DatasetBuilder();
    pbTesting.parseDatasetFromJson("datasetTrainingAndTest/test.json");
    ArrayList<Source> testSet = pbTesting.getSourceList();
    ///////////////////////////////////////////////


    // CREO LE MATRICI DI FEATURES E DI TARGET PER I DATASET DI TRAINING E TEST
    HashMap<String, ArrayList<Features>> featuresPapersTraining = featureExtractor.createFeaturesNormalizedInputDB(trainingSet,dictionaries);
    HashMap<String, ArrayList<Features>> targetsPapersTraining = featureExtractor.createTargetsInputDB(trainingSet, categories, dictionaries);
    HashMap<String, ArrayList<Features>> featuresPapersTest = featureExtractor.createFeaturesNormalizedInputDB(testSet,dictionaries);
    HashMap<String, ArrayList<Features>> targetsPapersTest = featureExtractor.createTargetsInputDB(testSet, categories, dictionaries);


    //CREO UN HASHMAP CHE PER OGNI DOCUMENTO HA COME CHIAVE UN OGGETTO IDANDTARGET E COME VALORE LA LISTA DELLE KEYWORDS
    HashMap<IdAndTarget,ArrayList<Features>> featuresPapersTrainingWithTarget = loadTargetForDosuments(featuresPapersTraining,targetsPapersTraining,categories);
    HashMap<IdAndTarget,ArrayList<Features>> featuresPapersTestWithTarget = loadTargetForDosuments(featuresPapersTest,targetsPapersTest,categories);
    
    //CREA LA FOLDER DA PASSARE A PYTHON PER CREARE IL DATASET
    createDatasetPython(featuresPapersTrainingWithTarget,categories,"train");
    createDatasetPython(featuresPapersTestWithTarget,categories,"test");


    //METODO CHE SCRIVE SU FILE I DATASET
    datastetOnFile(featuresPapersTraining,"featuresTraining.txt");
    datastetOnFile(targetsPapersTraining,"targetTraining.txt");
    datastetOnFile(featuresPapersTest,"featuresTest.txt");
    datastetOnFile(targetsPapersTest,"testTraining.txt");

    //SCRIVO LE MATRICI DI FEATURES SU FILE, IN MODO DA VEDERE I VALORI E MAGARI USARLI IN MATLAB
    writeMatrixOnFile(featuresPapersTraining, featuresPapersTest);


    numClasses=categories.size();


    System.out.println("Numero di paper per il train: "+ featuresPapersTraining.size()+"   Numero di paper per il test: "+featuresPapersTest.size());
    System.out.println("Numero di classi: "+numClasses);

    //BEST SETTING
    BestSetting bestSetting = trainingForBestConfig(featuresPapersTraining,targetsPapersTraining);

    System.out.println("BEST SETTING: \n activation function: "+bestSetting.getActivationFunction().toString()+" best Number Hidden Neurons: "+bestSetting.getNumHiddenNeurons());


    //RIPETO IL TRAINING ED IL TEST 3 VOLTE
    int numRipTest=3;
    while(numRipTest>0){
      //TRAINING
      NeuralNetwork network = training(featuresPapersTraining,targetsPapersTraining,bestSetting);

      //TEST
      test(network,featuresPapersTest,targetsPapersTest, testSet, categories); 
      numRipTest--;
    }
  }


  private static void createDatasetPython(HashMap<IdAndTarget, ArrayList<Features>> featuresPapersTrainingWithTarget, HashSet<String> categories, String fileName) throws IOException {
    //Per ogni categoria creo una folder
    String firstLine="labels,id";
    FileWriter writerCSV = new FileWriter("PythonCSV/dataset_"+fileName+".csv");
    for(int i=0;i<Configurator.numFeatures;i++)
      firstLine+=",F"+i;
    
    firstLine+="\n";
    
    writerCSV.write(firstLine);
    for(String category : categories){
      File dir = new File("PythonDatasets/"+category);
      dir.mkdir();
      for(IdAndTarget idAndTarget : featuresPapersTrainingWithTarget.keySet()){
        if(idAndTarget.getTarget().equals(category)){
          FileWriter writer = new FileWriter("PythonDatasets/"+category+"/"+idAndTarget.getId()+".txt");
          String keywordsToWrite=category+","+idAndTarget.getId()+",";
          Iterator iterator = featuresPapersTrainingWithTarget.get(idAndTarget).iterator();
          
          Features feature = (Features) iterator.next();
         
          keywordsToWrite+=feature.getScore();
          
          do{
            //keywordsToWrite+="";
            Features feature2 = (Features) iterator.next();
            keywordsToWrite+=","+feature2.getScore();
          }
          while(iterator.hasNext());
           
          keywordsToWrite+="\n";
          writerCSV.write(keywordsToWrite);
          writer.write(keywordsToWrite);
          writer.flush();
          writer.close();
        }
      }

    }

    writerCSV.flush();
    writerCSV.close();
  }


  //METODO CHE ASSOCIA OGNI DOCUMENTO ALLA CLASSE D?APPARTENENZA, SALVANDO ID E TARGET IN UN OGGETTO DI TIPO IdAndTarget
  private static HashMap<IdAndTarget, ArrayList<Features>> loadTargetForDosuments(HashMap<String, ArrayList<Features>> featuresPapersTraining,
      HashMap<String, ArrayList<Features>> targetsPapersTraining, HashSet<String> categories) {
    HashMap<IdAndTarget, ArrayList<Features>> toReturn = new HashMap<>();
    for(String key : featuresPapersTraining.keySet()){
      ArrayList<Features> targetsCurrentPaper = targetsPapersTraining.get(key);
      double currentValue=0;
      String currentTarget="";
      Iterator<String> iterator = categories.iterator();
      for(Features target : targetsCurrentPaper){
        String targetIter = iterator.next();
        if(target.getScore()>currentValue){          
          currentValue=target.getScore();
          currentTarget = targetIter;
        }

      }
      IdAndTarget  idAndTarget = new IdAndTarget(key,currentTarget);
      toReturn.put(idAndTarget, featuresPapersTraining.get(key));
      System.out.println(currentTarget);
    }


    return toReturn;
  }


  //METODO CHE SCRIVE IL DATASET DI INPUT SUL FILE 
  private static void datastetOnFile(HashMap<String, ArrayList<Features>> dataset, String pathFile) throws IOException {
    FileWriter writer = new FileWriter("datasetsTXT/"+pathFile);

    for(String idPaper : dataset.keySet()){
      String row = idPaper;
      for(Features f : dataset.get(idPaper)){
        row+=","+f.getScore();
      }
      writer.write(row+"\n");
    }

    writer.flush();
    writer.close();

  }


  //METODO CHE RESITUISCE UN HASHMAP DI CATEGORIA, PER OGNI CATEGORIA LA LISTA DI PAPER CHE APPARTENGONO
  private static HashMap<String, ArrayList<Source>> categoryListWithAssociatePapers(ArrayList<Source> trainingSet, HashSet<String> categories) {
    HashMap<String, ArrayList<Source>> categoryWithPapers = new HashMap<String, ArrayList<Source>>();

    for(String category : categories){
      ArrayList<Source> toInsert = new ArrayList<Source>();
      for(Source p: trainingSet){
        if(p.getCategoryList()!=null && !p.getCategoryList().isEmpty()){
          if(p.getCategoryList().get(0).getLabel().contains(category.replace("/", ""))){
            toInsert.add(p);
          }
          categoryWithPapers.put(category, toInsert);
        }
      }
    }
    return categoryWithPapers;
  }


  private static void writeMatrixOnFile(HashMap<String, ArrayList<Features>> featuresPapersTraining, HashMap<String, ArrayList<Features>> featuresPapersTest) throws IOException {
    FileWriter writerMatrixForTraining = new FileWriter("matrixForTraining.txt");
    FileWriter writerMatrixForTest = new FileWriter("matrixForTest.txt");

    for(String paperKey:featuresPapersTraining.keySet()){
      String toWrite="";
      for(int i=0;i<featuresPapersTraining.get(paperKey).size();i++){
        toWrite+=featuresPapersTraining.get(paperKey).get(i).getScore()+",";
      }
      writerMatrixForTraining.write(toWrite+"\n");
    }

    for(String paperKey:featuresPapersTest.keySet()){
      String toWrite="";
      for(int i=0;i<featuresPapersTest.get(paperKey).size();i++){
        toWrite+=featuresPapersTest.get(paperKey).get(i).getScore()+",";
      }
      writerMatrixForTest.write(toWrite+"\n");
    }

    writerMatrixForTest.flush();
    writerMatrixForTest.close();
    writerMatrixForTraining.flush();
    writerMatrixForTraining.close();

  }


  public static BestSetting trainingForBestConfig(HashMap<String,ArrayList<Features>> featuresPapers,HashMap<String,ArrayList<Features>> targetsPapers){

    BestSetting toReturn = new BestSetting();
    double score = Integer.MAX_VALUE;
    int hiddenNode=Configurator.numFeatures;
    //CICLO Sul numero di neuroni hidden: from numFeatures to numFeatures*2
    while(hiddenNode<=Configurator.numFeatures){
      //Ciclo su ogni funzione di attivazione
      for(TransferFunctionType function : TransferFunctionType.values()){
        System.out.println("Numero di features: "+Configurator.numFeatures);
        NeuralNetwork network;

        //SETTO LA FUNZIONE DI ATTIVAZIONE E IL NUMERO DI NEURONI HIDDEN, IL NUMERO DI ITERAZIONI MASSIME E L'ERRORE MASSIMO DA RAGGIUNGERE
        NeuronProperties neuronProperties = new NeuronProperties();
        neuronProperties.setProperty("useBias", true); 
        neuronProperties.setProperty("transferFunction",  function);
        int firstHiddenNode =hiddenNode+ (int) (hiddenNode+(Math.floor(hiddenNode/2)));
        network = new MultiLayerPerceptron(Configurator.numFeatures, firstHiddenNode, hiddenNode, numClasses);

        SupervisedLearning learningRule = (SupervisedLearning) network.getLearningRule();

        learningRule.setLearningRate(0.1d);
        learningRule.setMaxIterations(4000);
        learningRule.setMaxError(0.01); 

        DataSet trainingSet = new DataSet(Configurator.numFeatures, numClasses);

        //COSTRUISCO L'INPUT DA DARE ALLA RETE NEURALE
        for(String paperKey: featuresPapers.keySet()){

          double input[] = new double[featuresPapers.get(paperKey).size()];
          double output []= new double [numClasses];

          // COPIO LE FEATURES DEL PAPER CORRENTE IN UN ARRAY DI DOUBLE
          ArrayList<Features> featuresList = featuresPapers.get(paperKey);
          for(int k=0;k<featuresList.size();k++){
            input[k]=featuresList.get(k).getScore();
          }

          // copiamo i target di tipo Double nell array di tipo double
          for(int j=0; j<targetsPapers.get(paperKey).size();j++){
            output[j]=targetsPapers.get(paperKey).get(j).getScore();
          }

          // SE HO PRESO TUTTE LE FEATURES PER IL PAPER CORRENTE, LO AGGIUNGO AL DATASET
          if(input.length==Configurator.numFeatures){
            trainingSet.addRow(input,output);
          }
        }

        System.out.println("Current setup: function - "+function.toString()+" number hidden neuron - "+hiddenNode);
        //LANCIO IL TRAINING DELLA RETE NEURALE
        trainingSet.shuffle();
        System.out.println("Learning from training set...");

        network.learn(trainingSet);
        System.out.println("Learned from training set");
        System.out.println("Total error: " + learningRule.getTotalNetworkError());
        System.out.println("Iterations: " + learningRule.getCurrentIteration());
        System.out.println("Expected error: " + Math.sqrt(learningRule.getTotalNetworkError()));


        // SE LO SCORE OTTENUTO E' MIGLIORE DI QUELLO ATTUALE, MI CONSERVO LA NUOVA CONFIGURAIZIONE CHE DIVENTA LA BEST CONFIGURATION
        if(learningRule.getTotalNetworkError()<score){
          score= learningRule.getTotalNetworkError();
          toReturn.setNumHiddenNeurons(hiddenNode);
          toReturn.setActivationFunction(function);
        }

      }

      hiddenNode+=10;
    }
    return toReturn;


  }

  //metodo che fa il training della rete neurale
  public static NeuralNetwork training(HashMap<String,ArrayList<Features>> featuresPapers,HashMap<String,ArrayList<Features>> targetsPapers, BestSetting bestSetting){
    System.out.println("Numero di features: "+Configurator.numFeatures);
    NeuralNetwork network;
    System.out.println("BEST CONFIGURATION: \n    -Activation function: "+bestSetting.getActivationFunction()+"\n   - Number hiden Neurons: "+bestSetting.getNumHiddenNeurons());

    //SETTO LA FUNZIONE DI ATTIVAZIONE E IL NUMERO DI NEURONI HIDDEN, IL NUMERO DI ITERAZIONI MASSIME E L'ERRORE MASSIMO DA RAGGIUNGERE
    NeuronProperties neuronProperties = new NeuronProperties();
    neuronProperties.setProperty("useBias", true); 
    neuronProperties.setProperty("transferFunction",  bestSetting.getActivationFunction());
    int firstHiddenNode = bestSetting.getNumHiddenNeurons()+ (int) (bestSetting.getNumHiddenNeurons()+(Math.floor(bestSetting.getNumHiddenNeurons()/2)));
    network = new MultiLayerPerceptron(Configurator.numFeatures,firstHiddenNode,  bestSetting.getNumHiddenNeurons(), numClasses);
    SupervisedLearning learningRule = (SupervisedLearning) network.getLearningRule();
    learningRule.setLearningRate(0.1d);
    learningRule.setMaxIterations(4000);
    learningRule.setMaxError(0.01); 

    //COSTRUISCO L'INPUT DA DARE ALLA RETE NEURALE
    DataSet trainingSet = new DataSet(Configurator.numFeatures, numClasses);

    for(String paperKey: featuresPapers.keySet()){
      //Double[] array=featuresPapers.get(paperKey).toArray(new Double[featuresPapers.get(paperKey).size()]);

      double input[] = new double[featuresPapers.get(paperKey).size()];
      double output []= new double [numClasses];

      // COPIO LE FEATURES DEL PAPER CORRENTE IN UN ARRAY DI DOUBLE
      ArrayList<Features> featuresList = featuresPapers.get(paperKey);
      for(int k=0;k<featuresList.size();k++){
        input[k]=featuresList.get(k).getScore();
      }

      // copiamo i target di tipo Double nell array di tipo double

      for(int j=0; j<targetsPapers.get(paperKey).size();j++){
        output[j]=targetsPapers.get(paperKey).get(j).getScore();
      }
      // SE HO PRESO TUTTE LE FEATURES PER IL PAPER CORRENTE, LO AGGIUNGO AL DATASET
      if(input.length==Configurator.numFeatures){
        trainingSet.addRow(input,output);
      }
    }

    //LANCIO IL TRAINING DELLA RETE NEURALE
    trainingSet.shuffle();
    System.out.println("Learning from training set...");

    network.learn(trainingSet);
    System.out.println("Learned from training set");
    System.out.println("Total error: " + learningRule.getTotalNetworkError());
    System.out.println("Iterations: " + learningRule.getCurrentIteration());
    System.out.println("Expected error: " + Math.sqrt(learningRule.getTotalNetworkError()));


    return network;
  }

  // metodo che fa il test della rete neurale
  public static void test(NeuralNetwork network,HashMap<String,ArrayList<Features>> featuresPapers,HashMap<String,ArrayList<Features>> targetsPapers, ArrayList<Source> testSet, HashSet<String> classesUsed) throws IOException{

    double outputTest [] = new double [numClasses];
    FileWriter writerClassificationResults = new FileWriter("classificationResults.txt");

    //contains papers that haven't category in common with alchemy results.
    ArrayList<Source> paperWithoutAlchemyCategory = new ArrayList<Source>();

    double totalAccuracy=0;
    // CICLO SU OGNI PAPER SU CUI FARE TEST
    HashMap<String,ArrayList<String>> paperforCategoryClassified = new HashMap<String,ArrayList<String>>();
    for(Source p : testSet){
      //SE IL PAPER ESISTE MI COSTRUISCO L'INPUT DI FEATURES E TARGET DA DARE ALLA RETE NEURALE
      if(featuresPapers.get(p.getId())!=null){
        double input[] = new double[featuresPapers.get(p.getId()).size()];
        ArrayList<Features> featuresList = featuresPapers.get(p.getId());
        //COSTRUISCO L'INPUT DI FEATURES
        for(int i=0;i<featuresPapers.get(p.getId()).size(); i++){
          input[i]=featuresList.get(i).getScore();
        }

        //COSTRUISCO L'ARRAY DELLE ETICHETTE CORRETTE (ALCHEMY), SERVE PER LA STAMPA A VIDEO DEI RISULTATI (VECCHIA)
        for(int j=0; j<targetsPapers.get(p.getId()).size();j++){
          outputTest[j]=targetsPapers.get(p.getId()).get(j).getScore();
        }

        network.setInput(input);
        network.calculate();

        ArrayList<String> classes = new ArrayList<String>();

        for(String classe : classesUsed){
          classes.add(classe);
        }


        //STAMPO I RISULTATI SU FILE E SU CONSOLE

        //System.out.println("--------------PAPER: "+p.getId().replace("\n", ""));
        writerClassificationResults.write("--------------PAPER: "+p.getId().replace("\n", "")+"\n");

        int classSelected[] = returnClassesChoose(network.getOutput()); 

        //inserisco il paper nella lista di paper che appartengono alla stessa categoria
        if(paperforCategoryClassified.containsKey(classes.get(classSelected[0]))){
          paperforCategoryClassified.get(classes.get(classSelected[0])).add(p.getId());
        }else{
          ArrayList<String> toInsert = new ArrayList<String>();
          toInsert.add(p.getId());
          paperforCategoryClassified.put(classes.get(classSelected[0]), toInsert);
        }



        //System.out.println("CALCULATED: ");
        writerClassificationResults.write("CALCULATED: \n");
        //System.out.println(classes.get(classSelected[0])+" ("+network.getOutput()[classSelected[0]]+")");
        writerClassificationResults.write(classes.get(classSelected[0])+" ("+network.getOutput()[classSelected[0]]+")\n");
        //System.out.println(classes.get(classSelected[1])+" ("+network.getOutput()[classSelected[1]]+")"); 
        writerClassificationResults.write(classes.get(classSelected[1])+" ("+network.getOutput()[classSelected[1]]+")\n");
        //System.out.println(classes.get(classSelected[2])+" ("+network.getOutput()[classSelected[2]]+")");
        writerClassificationResults.write(classes.get(classSelected[2])+" ("+network.getOutput()[classSelected[2]]+")\n");

        if(p.getCategoryList()!=null && !p.getCategoryList().isEmpty()){
          ArrayList<CategoriesResult> categories = p.getCategoryList();
          //System.out.println("EXPECTED: ");
          writerClassificationResults.write("EXPECTED: \n");
          //System.out.println(categories.get(0).getLabel()+" ("+categories.get(0).getScore()+")");
          writerClassificationResults.write(categories.get(0).getLabel()+" ("+categories.get(0).getScore()+")\n");
          //System.out.println(categories.get(1).getLabel()+" ("+categories.get(1).getScore()+")");
          //writerClassificationResults.write(categories.get(1).getLabel()+" ("+categories.get(1).getScore()+")\n");
          //System.out.println(categories.get(2).getLabel()+" ("+categories.get(2).getScore()+")");
          //writerClassificationResults.write(categories.get(2).getLabel()+" ("+categories.get(2).getScore()+")\n");



          /* for(int k=0;k<network.getOutputsCount();k++){
         System.out.println("calss:" +classes.get(k));
         System.out.println("                calculated: "+network.getOutput()[k]); 
         System.out.println("                expected: " + outputTest[k]+"\n");
       }
       System.out.println("\n\n");*/
          boolean containsOneCategory = false;
          for(CategoriesResult cat : categories){
            containsOneCategory=false;
            if(cat.getLabel().contains(classes.get(classSelected[0]))){
              totalAccuracy+=1;
              containsOneCategory=true;
              break; 
            }
          }

          if(!containsOneCategory){
            paperWithoutAlchemyCategory.add(p);
          }
        }
      }
    }
    //PRINT INTO FILE PAPER THAT HAVEN'T COMMONS CATEGORY WITH ALCHEMY
    writerClassificationResults.write("\n\n PAPER WITHOUT ALCHEMY CATEGORY \n");
    for(Source p: paperWithoutAlchemyCategory){
      writerClassificationResults.write(p.getId()+"\n");
    }

    writerClassificationResults.flush();
    writerClassificationResults.close();

    System.out.println("\n\nCurrect class: "+totalAccuracy+" out of "+(testSet.size()));
    System.out.println("Total accuracy: "+totalAccuracy / (testSet.size()));


    FileWriter writerTestPaper = new FileWriter("keywordsPaperForTest.xml"); 
    writerTestPaper.write("<root>"+"\n");
    for(Source p: testSet){
      writerTestPaper.write("<paper>"+"\n");
      writerTestPaper.write("<id>"+"\n");
      writerTestPaper.write(p.getId().replace("\n", "")+"\n");
      writerTestPaper.write("</id>"+"\n");
      writerTestPaper.write("<keywords>"+"\n");
      if(p.getKeywordList()!=null){
        ArrayList<Keyword> keywordsList = p.getKeywordList();
        for(Keyword k : keywordsList){
          writerTestPaper.write(k.getText()+"\n");
        }
      }
      writerTestPaper.write("</keywords>"+"\n");
      writerTestPaper.write("</paper>"+"\n");
    }
    writerTestPaper.write("</root>"+"\n");
    writerTestPaper.flush();
    writerTestPaper.close();


    //Stampo le categorie con il numero di paper che vi appartengono
    System.out.println("\n\n NUMERO PAPER PER CLASSE");
    for(String key : paperforCategoryClassified.keySet()){
      System.out.println(key+": "+paperforCategoryClassified.get(key).size());
    }


  }


  private static int[] returnClassesChoose(double[] output) {
    int [] toReturn = new int[3];
    int size = output.length;
    int indexMax1=0;
    int indexMax2=0;
    int indexMax3=0;
    double max1=0;
    double max2=0;
    double max3=0;
    for(int i=0; i<size; i++){
      if(output[i]>max1){
        max1=output[i];
        indexMax1=i;
      } else if(output[i]>max2){
        max2=output[i];
        indexMax2=i;
      } else if(output[i]>max3){
        max3=output[i];
        indexMax3=i;
      }

    }

    toReturn[0]= indexMax1;
    toReturn[1]= indexMax2;
    toReturn[2]= indexMax3;
    return toReturn;

  }
}

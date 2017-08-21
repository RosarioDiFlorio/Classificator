package eu.innovation.engineering.keyword.extractor.maui.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.maui.filters.MauiFilter;
import eu.innovation.engineering.keyword.extractor.stemmers.PorterStemmer;
import eu.innovation.engineering.keyword.extractor.stemmers.Stemmer;
import eu.innovation.engineering.keyword.extractor.stopwords.Stopwords;
import eu.innovation.engineering.keyword.extractor.stopwords.StopwordsEnglish;
import eu.innovation.engineering.keyword.extractor.util.LanguageDetector;
import eu.innovation.engineering.keyword.extractor.vocab.Vocabulary;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;



/**
 * This class shows how to use Maui on a single document
 * or just a string of text.
 * @author alyona
 *
 */
public class MauiExtractor implements KeywordExtractor{

  /** Maui filter object */
  private MauiFilter extractionModel;
  private static final int topicsPerDocument = 10;
  private Vocabulary vocabulary = null;
  private Stemmer stemmer;
  private Stopwords stopwords;
  private String language = "en";
  private static final String dir = "Maui1.2/";
  private LanguageDetector languageDetector;

  
  
  /**
   * Constructor, which loads the data
   * @param dataDirectory - e.g. Maui's main directory (should has "data" dir in it)
   * @param vocabularyName - name of the rdf vocabulary
   * @param modelName - name of the model
   */
  public MauiExtractor(String dataDirectory, String vocabularyName, String modelName) {

    languageDetector = new LanguageDetector();
    stemmer = new PorterStemmer();
    String englishStopwords = dataDirectory + "data/stopwords/stopwords_en.txt";
    stopwords = new StopwordsEnglish(englishStopwords);
    String vocabularyDirectory = dataDirectory +  "data/vocabularies/";
    String modelDirectory = dataDirectory +  "data/models";
    if(!vocabularyName.equals("none"))
      loadVocabulary(vocabularyDirectory, vocabularyName);
    loadModel(modelDirectory, modelName, vocabularyName);
  }

  /**
   * Loads a vocabulary from a given directory
   * @param vocabularyDirectory
   * @param vocabularyName
   */
  public void loadVocabulary(String vocabularyDirectory, String vocabularyName) {
    if (vocabulary != null)
      return;
    try {
      vocabulary = new Vocabulary(vocabularyName, "skos", vocabularyDirectory);
      vocabulary.setStemmer(stemmer);
      vocabulary.setStopwords(stopwords);
      vocabulary.setLanguage(language);
      vocabulary.initialize();
    } catch (Exception e) {
      System.err.println("Failed to load vocabulary!");
      e.printStackTrace();
    }
  }

  /**
   * Loads the model
   * @param modelDirectory
   * @param modelName
   * @param vocabularyName
   */
  public void loadModel(String modelDirectory, String modelName, String vocabularyName) {

    try {
      BufferedInputStream inStream = new BufferedInputStream(
          new FileInputStream(modelDirectory + "/" + modelName));
      ObjectInputStream in = new ObjectInputStream(inStream);

      extractionModel = (MauiFilter) in.readObject();
      in.close();
    } catch (Exception e) {
      System.err.println("Failed to load model!");
      e.printStackTrace();
    }

    extractionModel.setVocabularyName(vocabularyName);
    extractionModel.setVocabularyFormat("skos");
    extractionModel.setDocumentLanguage(language);
    extractionModel.setStemmer(stemmer);
    extractionModel.setStopwords(stopwords);
    extractionModel.setVocabulary(vocabulary);

  }

  /**
   * Triggers topic extraction from a text file
   * @param filePath
   * @param numberOfTopics
   * @return
   * @throws Exception
   * @Deprecated Use extractKeywordsFromFile
   */
  public ArrayList<String> extractTopicsFromFile(String filePath, int numberOfTopics) throws Exception {
    File documentTextFile = new File(filePath);
    String documentText = FileUtils.readFileToString(documentTextFile);
    return extractTopicsFromText(documentText, numberOfTopics);
  }

  /**
   * Main method to extract the main topics from a given text
   * @param text
   * @param topicsPerDocument
   * @return
   * @throws Exception
   * @Deprecated Use extractKeywordsFrom Text
   */
  public ArrayList<String> extractTopicsFromText(String text, int topicsPerDocument) throws Exception {

    if (text.length() < 5) {
      throw new Exception("Text is too short!");
    }

    extractionModel.setWikipedia(null);

    FastVector atts = new FastVector(3);
    atts.addElement(new Attribute("filename", (FastVector) null));
    atts.addElement(new Attribute("doc", (FastVector) null));
    atts.addElement(new Attribute("keyphrases", (FastVector) null));
    Instances data = new Instances("keyphrase_training_data", atts, 0);

    double[] newInst = new double[3];

    newInst[0] = data.attribute(0).addStringValue("inputFile");
    newInst[1] = data.attribute(1).addStringValue(text);
    newInst[2] = Instance.missingValue();
    data.add(new Instance(1.0, newInst));

    extractionModel.input(data.instance(0));

    data = data.stringFreeStructure();
    Instance[] topRankedInstances = new Instance[topicsPerDocument];
    Instance inst;
    // Iterating over all extracted keyphrases (inst)
    while ((inst = extractionModel.output()) != null) {

      int index = (int) inst.value(extractionModel.getRankIndex()) - 1;

      if (index < topicsPerDocument) {
        topRankedInstances[index] = inst;
      }
    }

    ArrayList<String> topics = new ArrayList<String>();

    for (int i = 0; i < topicsPerDocument; i++) {
      if (topRankedInstances[i] != null) {
        String topic = topRankedInstances[i].stringValue(extractionModel
            .getOutputFormIndex());

        topics.add(topic);
      }
    }
    extractionModel.batchFinished();
    return topics;
  }

  /**
   * Main method to extract the main topics from a given text
   * @param text
   * @param topicsPerDocument
   * @return
   * @throws Exception
   */
  public ArrayList<Keyword> extractKeywordsFromText(String text, int topicsPerDocument) throws Exception {

    if (text.length() < 5) {
      throw new Exception("Text is too short!");
    }

    extractionModel.setWikipedia(null);
    extractionModel.setFrequencyFeatures(true);
    FastVector atts = new FastVector(3);
    atts.addElement(new Attribute("filename", (FastVector) null));
    atts.addElement(new Attribute("doc", (FastVector) null));
    atts.addElement(new Attribute("keyphrases", (FastVector) null));
    Instances data = new Instances("keyphrase_training_data", atts, 0);

    double[] newInst = new double[3];

    newInst[0] = data.attribute(0).addStringValue("inputFile");
    newInst[1] = data.attribute(1).addStringValue(text);
    newInst[2] = Instance.missingValue();
    data.add(new Instance(1.0, newInst));

    extractionModel.input(data.instance(0));

    data = data.stringFreeStructure();
    Instance[] topRankedInstances = new Instance[topicsPerDocument];
    Instance inst;
    // Iterating over all extracted keyphrases (inst)
    while ((inst = extractionModel.output()) != null) {

      int index = (int) inst.value(extractionModel.getRankIndex()) - 1;

      if (index < topicsPerDocument) {
        topRankedInstances[index] = inst;
      }
    }

    ArrayList<Keyword> toReturn = new ArrayList<Keyword>();
    for (int i = 0; i < topicsPerDocument; i++) {
      if (topRankedInstances[i] != null) {
        String topic = topRankedInstances[i].stringValue(extractionModel
            .getOutputFormIndex());

        double invfreq = topRankedInstances[i].value(extractionModel.getOutputFormat().attribute("IDF"));
        double freq = topRankedInstances[i].value(extractionModel.getOutputFormat().attribute("Term_frequency"));
        double tdidf = topRankedInstances[i].value(extractionModel.getOutputFormat().attribute("TFxIDF"));
        Keyword k = new Keyword();
        k.setText(topic);
        k.setRelevance(tdidf);
        //k.setInverseFrequency(invfreq);
        //k.setFrequency(freq);
        toReturn.add(k);
      }
    }

    extractionModel.batchFinished();

    return toReturn;
  } 

  /**
   * Triggers topic extraction from a text file
   * @param filePath
   * @param numberOfTopics
   * @return
   * @throws Exception
   * 
   */
  public ArrayList<Keyword> extractKeywordsFromFile(String filePath, int numberOfTopics) throws Exception {
    File documentTextFile = new File(filePath);
    String documentText = FileUtils.readFileToString(documentTextFile);
    return extractKeywordsFromText(documentText, numberOfTopics);
  }

  public static void main(String[] args){

      main1(args);
    
  }


  /**
   * Main method for testing MauiWrapper
   * Add the path to a text file as command line argument
   * @param args
   */
  public static void main1(String[] args) {

    String vocabularyName = "none";
    String modelName = "InnenModel";
    String dataDirectory = "/Maui1.2/data/";
    System.out.println("Start");
    MauiExtractor wrapper = new MauiExtractor(dataDirectory, vocabularyName, modelName);

    try {
      String parentFolder = "/Maui1.2/data/mauiDataset/description/";
      File folder = new File(parentFolder);
      File[] listOfFiles = folder.listFiles();
      PrintWriter w = new PrintWriter("result_innen_description_test.txt");
      for (int i = 0; i < listOfFiles.length; i++) {
        File file = listOfFiles[i];
        if (file.isFile() && file.getName().endsWith(".txt")) {
          try{
            ArrayList<String> keywords = wrapper.extractTopicsFromFile(parentFolder+file.getName(), 8);


            w.println("==========================");
            w.println(file.getName().replace(".txt", ""));
            for (String keyword : keywords) {
              w.println(keyword);
            }
          }catch (Exception e) {
            // TODO: handle exception
            continue;
          }


        } 

      }
      w.flush();
      w.close();
      System.out.println("Done");

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public List<Keyword> extractKeywordsFromText(List<String> toAnalyze) throws Exception {
    // TODO Auto-generated method stub
    String text = languageDetector.filterForLanguage(toAnalyze, "en");
    

    if (text.length() < 5) {
      throw new Exception("Text is too short!");
    }

    extractionModel.setWikipedia(null);
    extractionModel.setFrequencyFeatures(true);
    FastVector atts = new FastVector(3);
    atts.addElement(new Attribute("filename", (FastVector) null));
    atts.addElement(new Attribute("doc", (FastVector) null));
    atts.addElement(new Attribute("keyphrases", (FastVector) null));
    Instances data = new Instances("keyphrase_training_data", atts, 0);

    double[] newInst = new double[3];

    newInst[0] = data.attribute(0).addStringValue("inputFile");
    newInst[1] = data.attribute(1).addStringValue(text);
    newInst[2] = Instance.missingValue();
    data.add(new Instance(1.0, newInst));

    extractionModel.input(data.instance(0));

    data = data.stringFreeStructure();
    Instance[] topRankedInstances = new Instance[topicsPerDocument];
    Instance inst;
    // Iterating over all extracted keyphrases (inst)
    while ((inst = extractionModel.output()) != null) {

      int index = (int) inst.value(extractionModel.getRankIndex()) - 1;

      if (index < topicsPerDocument) {
        topRankedInstances[index] = inst;
      }
    }

    ArrayList<Keyword> toReturn = new ArrayList<Keyword>();
    for (int i = 0; i < topicsPerDocument; i++) {
      
      if (topRankedInstances[i] != null) {
        String topic = topRankedInstances[i].stringValue(extractionModel
            .getOutputFormIndex());

        double invfreq = topRankedInstances[i].value(extractionModel.getOutputFormat().attribute("IDF"));
        double freq = topRankedInstances[i].value(extractionModel.getOutputFormat().attribute("Term_frequency"));
        double tdidf = topRankedInstances[i].value(extractionModel.getOutputFormat().attribute("TFxIDF"));
        Keyword k = new Keyword();
        k.setText(topic);
        k.setRelevance(tdidf);
        //k.setInverseFrequency(invfreq);
        //k.setFrequency(freq);
        toReturn.add(k);
      }
    }

    extractionModel.batchFinished();

    return toReturn;
    
    
  }





}

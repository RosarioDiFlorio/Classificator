package eu.innovationengineering.extractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovationengineering.persistence.SQLiteVectors;
import eu.innovationengineering.utilities.Lemmatizer;
import eu.innovationengineering.utilities.PickleReader;
import eu.innovationengineering.utilities.Result;
import eu.innovationengineering.utilities.Similarities;
import eu.innovationengineering.utilities.SpringMainLauncher;
import eu.innovationengineering.utilities.StopWords;
import eu.innovationengineering.utilities.Word2Vec;

public class GlossaryBuilder  extends SpringMainLauncher implements InitializingBean {

  private static final Logger logger = LoggerFactory.getLogger(GlossaryBuilder.class);
  private String pklFolderPath;
  private String datasetFolder;
  private String glossariesFolder;

  @Autowired
  private Word2Vec word2Vec;

  @Autowired
  private Lemmatizer lemmatizer;

  @Autowired
  private StopWords stopWords;

  @Autowired
  @Qualifier("wordVectors")
  private SQLiteVectors wordVectDB;

  @Autowired
  @Qualifier("glossariesVectors")
  private SQLiteVectors glossariesVecDB;

  @Autowired
  @Qualifier("hierarchyVectors")
  private SQLiteVectors hierarchyVectDB;

  private Map<String, float[]> vectorsWord;
  private Map<String, float[]> glossariesVector;
  private Map<String, float[]> hierarchyVector;
  private ObjectMapper mapper;

  public GlossaryBuilder(String pklFolder,String datasetFolder,String glossariesFolder){
    this.pklFolderPath = pklFolder+"/";
    this.datasetFolder = datasetFolder+"/";
    this.glossariesFolder = glossariesFolder+"/";
    this.mapper = new ObjectMapper();
  }

  public static void main(String[] args) throws Exception{
    ObjectMapper mapper = new ObjectMapper();

    mainWithSpring(
        context -> {
          GlossaryBuilder glossary = context.getBean(GlossaryBuilder.class);
          /*Map<String, List<Result>> glossaries = glossary.generateGlossaries(0.5);
          mapper.writerWithDefaultPrettyPrinter().writeValue(new File("app/glossaries/glossaries.json"), glossaries);

          Map<String, float[]> glossariesVector = glossary.initGlossariesVector(glossaries);
          mapper.writerWithDefaultPrettyPrinter().writeValue(new File("app/glossaries/glossariesVectors.json"), glossariesVector);*/
          glossary.train();
        },
        args,
        "classpath:spring/properties-config.xml","classpath:spring/word2vec.xml" ,
        "classpath:spring/service-config.xml","classpath:spring/utilities-config.xml",
        "classpath:spring/persistence-config.xml");


  }



  public Map<String, float[]> train() throws Exception{
    Set<String> words = new HashSet<>(vectorsWord.keySet());
    words.addAll(getPickleWords());
    words.addAll(getWordsFromFiles());
    words.removeAll(vectorsWord.keySet());
    initWordVectors(new ArrayList<>(words)); 
    afterPropertiesSet();
    words.clear();  
    return vectorsWord;
  }




  //leggo tutte le parole contenute nei file pkl.
  /*
   * Non credo abbia molto senso leggere le parole solamente dai pkl.
   */
  public  Set<String> getPickleWords() throws IOException, InterruptedException{
    StopWords stopWords = new StopWords("en");
    PickleReader pickle = new PickleReader();
    Set<String> words = new HashSet<String>();
    File pklFolder = new File(pklFolderPath);
    File[] pklFiles = pklFolder.listFiles();
    for(File pkl: pklFiles){

      Map<String, String> pklDict = pickle.loadPklFile(pkl.getCanonicalPath());
      pklDict.keySet().stream().forEach(word->words.addAll(stopWords.cleanText(word)));
      System.out.println(pkl.getCanonicalPath());
      System.gc ();
      System.runFinalization ();
    }
    Set<String> toReturn = new HashSet<>();
    for(String word: words){
      toReturn.addAll(lemmatizer.lemmatize(word));
    }
    return words;
  }


  public Set<String> getWordsFromFiles() throws IOException{
    Set<String> files = listAllFiles(datasetFolder, new HashSet<>());
    Set<String> words = new HashSet<String>();
    System.out.println(files.size());
    int count = 0;
    for(String file : files){
      count++;
      // FileReader reads text files in the default encoding.
      FileReader fileReader =  new FileReader(file);

      // Always wrap FileReader in BufferedReader.
      try(BufferedReader bufferedReader = 
          new BufferedReader(fileReader);){

        String line;
        while((line = bufferedReader.readLine()) != null) {
          List<String> stemmedLine = lemmatizer.lemmatize(line);
          for(String stemmed:stemmedLine){
            words.addAll(stopWords.cleanText(stemmed));
          }

        }   
      }
      System.out.println(((count*100)/files.size())+"%   -   "+words.size());
    }
    return words;    
  }

  /*
   * Inizializzo la mappa dei vettori di ogni parola del training
   */
  public Map<String,float[]> initWordVectors(List<String> words) throws IOException, InterruptedException, SQLException{
    vectorsWord = wordVectDB.getAllVectors();
    words.removeAll(vectorsWord.keySet());
    System.out.println(words.size());

    Map<String,float[]> supportVectorMap = new HashMap<>();
    List<String> batch = new ArrayList<>();
    List<List<String>> toVectorize = new ArrayList<>();
    if(!words.isEmpty()){
      for(String word : words){
        if(word.length()>2){
          batch.add(word); 
          if(batch.size() >= 2000){

            for(String w:batch){
              List<String> wrapword = new ArrayList<>();
              wrapword.add(w);
              toVectorize.add(wrapword);
            }
            float[][] vectorized = word2Vec.returnVectorsFromTextList(toVectorize);
            for(int i = 0;i<batch.size();i++){
              supportVectorMap.put(batch.get(i), vectorized[i]);
            }
            batch.clear();
            toVectorize.clear();   
            wordVectDB.setAutoCommit(false);
            wordVectDB.insertVectors(supportVectorMap);
            wordVectDB.commitConnection();
            wordVectDB.setAutoCommit(true);
            supportVectorMap.clear();
          }
        }

      }
      for(String w:batch){
        List<String> wrapword = new ArrayList<>();
        wrapword.add(w);
        toVectorize.add(wrapword);
      }
      float[][] vectorized = word2Vec.returnVectorsFromTextList(toVectorize);
      for(int i = 0;i<batch.size();i++){
        supportVectorMap.put(batch.get(i), vectorized[i]);
      }
      batch.clear();
      toVectorize.clear();

      wordVectDB.setAutoCommit(false);
      wordVectDB.insertVectors(supportVectorMap);
      wordVectDB.commitConnection();
      wordVectDB.setAutoCommit(true);
      supportVectorMap.clear();
    }
    vectorsWord = wordVectDB.getAllVectors();
    return vectorsWord;
  }



  public void initHierarchyVectors() throws JsonParseException, JsonMappingException, IOException{
    hierarchyVector = new HashMap<String, float[]>();
    Map<String,List<String>> categoriesMap = mapper.readValue(new File("volume_shared/categories.json"), new TypeReference<Map<String,List<String>>>() {});
    for(String category: categoriesMap.keySet()){
      List<List<String>> toVectorize = new ArrayList<>();
      List<String> categoryList =  new ArrayList<>();
      stopWords.cleanText(category).forEach(str->categoryList.addAll(lemmatizer.lemmatize(str)));
      categoriesMap.get(category).forEach(str->categoryList.addAll(stopWords.cleanText(str)));
      //categoriesMap.get(category).forEach(str-> categoryList.addAll(lemmatizer.lemmatize(str)));
      toVectorize.add(categoryList);
      float[] toCompare = word2Vec.returnVectorsFromTextList(toVectorize)[0];
      hierarchyVector.put(category, toCompare);
    }
  }

  public Map<String, List<Result>> generateGlossaries(double threshold) throws IOException{
    Map<String, List<Result>> glossaries = new HashMap<String, List<Result>>();
    for(String category: hierarchyVector.keySet()){
      float[] toCompare = hierarchyVector.get(category);
      glossaries.put(category, new ArrayList<>(filteredWords(toCompare, threshold)));
    }
    return glossaries;
  }

  public Set<Result> filteredWords(float[] toCompare,double threshold){
    Set<Result> candidates = new HashSet<Result>();    
    for(String word:vectorsWord.keySet()){
      double sim = Similarities.cosineSimilarity(toCompare, vectorsWord.get(word));
      if(sim > threshold)
        candidates.add(new Result(word, sim));
    }
    return candidates;
  }

  public Map<String,float[]> initGlossariesVector(Map<String,List<Result>> glossary) throws IOException, InterruptedException, SQLException{
    Map<String, float[]> glossaryVector = new HashMap<>();
    List<List<String>> toVectorize = new ArrayList<>();
    for(String category : glossary.keySet()){
      StringBuilder sb = new StringBuilder();
      System.out.println(category);
      glossary.get(category).forEach(s->sb.append(s.getLabel()+" "));
      toVectorize.add(stopWords.cleanText(sb.toString()));
      float[] catVector = word2Vec.returnVectorsFromTextList(toVectorize)[0];
      glossaryVector.put(category, catVector);
      toVectorize.clear();
    }
    glossariesVecDB.setAutoCommit(false);
    getGlossariesVecDB().insertVectors(glossaryVector);
    glossariesVecDB.commitConnection();
    glossariesVecDB.setAutoCommit(true);
    return glossaryVector;
  }

  /**
   * List all files from a directory and its subdirectories
   * @param directoryName to be listed
   * @return 
   */
  public Set<String> listAllFiles(String directoryName, Set<String> fileList){
    //    directoryName = directoryName.replace("/", "\\");
    //    System.out.println(directoryName);
    File directory = new File(directoryName);
    //get all the files from a directory
    File[] fList = directory.listFiles();
    for (File file : fList){
      if (file.isFile()){
        //System.out.println(file.getAbsolutePath());
        fileList.add(file.getAbsolutePath());
      } else if (file.isDirectory()){
        listAllFiles(file.getAbsolutePath(),fileList);
      }
    }
    return fileList;
  }

  public Map<String, float[]> getGlossariesVector() {
    return glossariesVector;
  }

  public SQLiteVectors getWordVectors() {
    return wordVectDB;
  }

  public void setWordVectors(SQLiteVectors wordVectors) {
    this.wordVectDB = wordVectors;
  }

  public SQLiteVectors getGlossariesVecDB() {
    return glossariesVecDB;
  }

  public void setGlossariesVecDB(SQLiteVectors glossariesVecDB) {
    this.glossariesVecDB = glossariesVecDB;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    initHierarchyVectors();
    initWordVectors(new ArrayList<>());
    initGlossariesVector(generateGlossaries(0.5));
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("app/glossaries/glossariesVectors.json"), glossariesVector);
  }





}

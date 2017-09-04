package eu.innovation.engineering.keyword.extractor.innen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.util.LanguageDetector;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.similarity.bean.WordFrequency;
import eu.innovationengineering.similarity.keyword.runnable.KeywordExtractionRunnable;
import eu.innovationengineering.similarity.util.function.ExceptionThrowingConsumer;
import eu.innovationengineering.solrclient.auth.api.TextAnalyzer;

public class InnenExtractor implements KeywordExtractor {

  private static final Logger log = LoggerFactory.getLogger(InnenExtractor.class);



  private Set<String> blackList;

  private String stopWordPath= "data/stopwords/stopwords_en.txt";
  private String mainDirectory = "";
  private LanguageDetector languageDetector;


  public InnenExtractor(String mainDirectory){
    this.mainDirectory = mainDirectory;
    this.stopWordPath = this.mainDirectory + stopWordPath;
    languageDetector = new LanguageDetector();

  }

  @Override
  public List<Keyword> extractKeywordsFromText(List<String> texts, int numKeywordsToReturn) throws LanguageException{
    ArrayList<Keyword> toReturn  = new ArrayList<>();
    String[] configLocations = new String[] {"classpath:/spring/base-context.xml","classpath:/spring/solrclient-beans.xml", "classpath:/spring/oat-analysis.xml", "classpath:/spring/keywords-context.xml"};
    executeInSpringContext(configLocations, context -> {


      String toAnalyze = languageDetector.filterForLanguage(texts, "en");
      TextAnalyzer textAnalyzer = context.getBean(TextAnalyzer.class);

      KeywordExtractionRunnable analyzerRunnable = context.getBean("keywordExtractionRunnable", KeywordExtractionRunnable.class);
      analyzerRunnable.setBlackList(this.getBlackList());
      analyzerRunnable.setTexts(toAnalyze);
      analyzerRunnable.setNdcElements();
      analyzerRunnable.setDataType("grant");
      analyzerRunnable.setExternalId(""+toAnalyze.hashCode());
      List<WordFrequency> keywords = new ArrayList<>();
      analyzerRunnable.setKeywordsConsumer(dwk -> dwk.getKeywords().forEach(keywords::add));
      analyzerRunnable.run();


      int count=0;
      for(WordFrequency word: keywords){
        //if(count<numKeywordsToReturn){
          count++;
          Keyword k = new Keyword();
          k.setText(word.getWord());
          k.setRelevance((double) word.getFrequency());
          toReturn.add(k);
        /*}
        else
          break;*/
      }
    });

    //System.out.println(toReturn.size());
    return toReturn;
  }


  private Set<String> getBlackList(){
    if (this.blackList == null) {
      this.blackList = new HashSet<String>();
      File txt = new File(getStopWordPath());  
      InputStreamReader is;
      String sw = null;
      try {
        is = new InputStreamReader(new FileInputStream(txt), "UTF-8");
        BufferedReader br = new BufferedReader(is);             
        while ((sw=br.readLine()) != null)  {
          this.blackList.add(sw);   
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return blackList;
  }

  private static void executeInSpringContext( String configLocations[], ExceptionThrowingConsumer<ClassPathXmlApplicationContext> action) {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    GenericApplicationContext cmdArgCxt = new GenericApplicationContext(beanFactory);
    // Must call refresh to initialize context
    cmdArgCxt.refresh();

    try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocations, true, cmdArgCxt)) {
      action.accept(context);
    }
    catch (Throwable e) {
      log.error("An error occurred", e);
    }
  }

  public String getStopWordPath() {
    return stopWordPath;
  }

  public void setStopWordPath(String stopWordPath) {
    this.stopWordPath = stopWordPath;
  }










}

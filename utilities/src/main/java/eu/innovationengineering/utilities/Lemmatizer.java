package eu.innovationengineering.utilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;



/**
 * @author Rosario
 *
 */
public class Lemmatizer {

  /**
   * StanfordCoreNLP loads a lot of models, so you probably
   *   only want to do this once per execution 
   */
  private StanfordCoreNLP pipeline;
  private Properties props;

  /**
   * Create StanfordCoreNLP object properties, with POS tagging
   * (required for lemmatization), and lemmatization
   */
  public Lemmatizer() {

  }

  public void initPipeline(){
    if(pipeline == null){
      if(props == null)
        props = new Properties();
      props.put("annotators", "tokenize, ssplit, pos,lemma");
      // StanfordCoreNLP loads a lot of models, so you probably
      // only want to do this once per execution
      this.pipeline = new StanfordCoreNLP(props);
    }
  }


  public List<String> lemmatize(String documentText)
  {
    initPipeline();

    List<String> lemmas = new ArrayList<String>();

    // create an empty Annotation just with the given text
    Annotation document = new Annotation(documentText);
    
    // run all Annotators on this text
    pipeline.annotate(document);
    
    // Iterate over all of the sentences found
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    
    for(CoreMap sentence: sentences) {
      // Iterate over all tokens in a sentence
      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
        // Retrieve and add the lemma for each word into the list of lemmas
        lemmas.add(token.get(LemmaAnnotation.class));
        //System.out.println(token);
      }
    }



    return lemmas;
  }



}


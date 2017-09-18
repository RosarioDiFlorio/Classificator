package eu.innovationengineering.Tester;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Lemmatizer {
  
      protected StanfordCoreNLP pipeline;

      public static void main(String[] args){
        String test= "he first person in line for Hillary Clinton’s book signing in New York said he had not voted at all in the presidential election – and that he regretted it."
            +"Brian Maisonet, a 29-year-old from Brooklyn, said he had arrived at 3.30pm on Monday and waited outside the bookstore overnight to meet Clinton at a Tuesday afternoon event for her book What Happened, a punchy and personal account of her stunning defeat by Donald Trump.";    
        Lemmatizer lemmatizer = new Lemmatizer();
        List<String> lemmatized = lemmatizer.lemmatize(test);
        System.out.println(lemmatized);
      }
      
      
      public Lemmatizer() {
          // Create StanfordCoreNLP object properties, with POS tagging
          // (required for lemmatization), and lemmatization
          Properties props;
          props = new Properties();
          props.put("annotators", "tokenize, ssplit, pos,lemma,ner, depparse,parse");

          // StanfordCoreNLP loads a lot of models, so you probably
          // only want to do this once per execution
          this.pipeline = new StanfordCoreNLP(props);
      }

      public List<String> lemmatize(String documentText)
      {
          List<String> lemmas = new LinkedList<String>();

          // create an empty Annotation just with the given text
          Annotation document = new Annotation(documentText);

          // run all Annotators on this text
          this.pipeline.annotate(document);
          
          // Iterate over all of the sentences found
          List<CoreMap> sentences = document.get(SentencesAnnotation.class);
          for(CoreMap sentence: sentences) {
              // Iterate over all tokens in a sentence
              for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                  // Retrieve and add the lemma for each word into the list of lemmas
                  lemmas.add(token.get(LemmaAnnotation.class));
                  System.out.println(token);
              }
          }
          


          return lemmas;
      }
      
     

  }


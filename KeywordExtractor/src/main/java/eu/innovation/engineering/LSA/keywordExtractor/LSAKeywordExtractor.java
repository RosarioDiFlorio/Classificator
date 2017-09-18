package eu.innovation.engineering.LSA.keywordExtractor;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;
import eu.innovation.engineering.keyword.extractor.util.CleanUtilis;
import eu.innovationengineering.lang.ISO_639_1_LanguageCode;
import eu.innovationengineering.lang.exceptions.LanguageException;
import eu.innovationengineering.nlp.analyzer.stanfordnlp.StanfordnlpAnalyzer;
import eu.innovationengineering.nlp.beans.AnnotatedWord;
import eu.innovationengineering.nlp.beans.CompleteSentenceAnalysis;
import eu.innovationengineering.nlp.beans.oat.SentenceChunk;

/**
 * @author Rosario
 * @author Luigi
 *
 */
public class LSAKeywordExtractor implements KeywordExtractor {  
  /* (non-Javadoc)
   * @see eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor#extractKeywordsFromText(java.util.List, int)
   */
  @Override
  public List<Keyword> extractKeywordsFromText(List<String> toAnalyze, int numKeywordsToReturn) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @param text
   * @return
   * @throws LanguageException 
   */
  public static List<SentenceChunk> createChunkFromText(String text) throws LanguageException{
    text = text.toLowerCase();
    String[] cleanedString = text.split(" ");
    text = "";
    for(String s : cleanedString){
      if(!CleanUtilis.getBlackList().contains(s))
        text+= s+" ";
    }
    StanfordnlpAnalyzer nlpAnalyzer = new StanfordnlpAnalyzer();
    CompleteSentenceAnalysis results = nlpAnalyzer.executeCompleteSentenceAnalysis(text, ISO_639_1_LanguageCode.ENGLISH, true, true);
    List<SentenceChunk> chunkList = cleanChunks(results.getChunks());
    
    //debug print
    List<List<String>> toprint = chunkList.stream().map(sc->sc.getWords().stream().map(w->w.getWord()).collect(Collectors.toList())).collect(Collectors.toList());
    toprint.stream().forEach(System.out::println);
    
    
    return chunkList;
  }

  /**
   * @param chunks
   * @return
   */
  private static List<SentenceChunk> cleanChunks(List<SentenceChunk> chunks){
    Set<String> stopwords = CleanUtilis.getBlackList();

    for(Iterator<SentenceChunk> it = chunks.iterator();it.hasNext();){
      SentenceChunk sc = it.next();
      Iterator<AnnotatedWord> iter = sc.getWords().iterator();
      while(iter.hasNext()){
        AnnotatedWord word = iter.next();
        if(stopwords.contains(word.getWord()) && word.getWord().length()<=2)
          iter.remove();
      }
      if(sc.getWords().isEmpty())
        it.remove();
      //System.out.println(sc.getWords().stream().map(an->an.getWord()).collect(Collectors.toList()).toString());
    }
    return chunks;
  }

  /**
   * @param chunks
   * @return
   */
  public static MatrixRepresentation buildMatrixA(List<String> chunks){
    return null;
  }

  /**
   * @param <E>
   * @return toDefine
   */
  public static SVDMatrix SVD(MatrixRepresentation matrixA){  
    return null;
  }


  private static  List<Keyword> getKeywordList(MatrixRepresentation matrixA, SVDMatrix SVDResult){
    return null;
  }


  private static float Tf(String word,List<String>chunks){
    return 0;
  }

  private static float Isf(String word,List<String> chunks){
    return 0;
  }


}

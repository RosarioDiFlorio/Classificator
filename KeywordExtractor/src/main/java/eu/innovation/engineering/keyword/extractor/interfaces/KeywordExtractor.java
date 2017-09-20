package eu.innovation.engineering.keyword.extractor.interfaces;

import java.util.List;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

public interface KeywordExtractor {
  
  /**
   * @param toAnalyze - List of strings to analyze
   * @param numKeywordsToReturn - maximum number of keyword to return
   * @return The list of the keywords extracted from the list of string analyzed.
   * @throws Exception
   */
  public List<List<Keyword>> extractKeywordsFromTexts(List<String> toAnalyze, int numKeywordsToReturn) throws Exception;
  


}

package eu.innovation.engineering.keyword.extractor;

import java.util.List;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

public interface KeywordExtractor {
  
  public List<Keyword> extractKeywordsFromText(List<String> toAnalyze) throws Exception;

}

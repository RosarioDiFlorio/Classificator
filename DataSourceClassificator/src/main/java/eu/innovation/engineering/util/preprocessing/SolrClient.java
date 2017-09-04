package eu.innovation.engineering.util.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;

import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;

public class SolrClient {
  
  public static void main(String[] args) throws Exception{
    requestNPaper(200,200);    
  }

  public static void requestNPaper(int firstPaperToJump,int numSourceRequest) throws Exception{
    String cursorMark="*";
    String url = "http://192.168.200.81:8080/solr4/technical_papers/select?q=*%3A*&sort=id+asc&fl=id%2Cdc_title%2Cdc_description&wt=json&indent=true&cursorMark=";
    KeywordExtractor extractorInnen = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);

    int numSourceToSave = 0;
    JsonParser parserJson = new JsonParser();

    //creo il file 
    int count = 0;
    ArrayList<Source> sourceList = new ArrayList<Source>();
    
    //Salto i primi paper
    int paperJumped =0;
    while(paperJumped<firstPaperToJump){
      StringBuffer response = requestSOLR(url+cursorMark);
      paperJumped+=10;
      cursorMark = parserJson.parse(response.toString()).getAsJsonObject().get("nextCursorMark").getAsString();
    }
    
    //prendo i paper
    while (numSourceToSave<numSourceRequest){
      numSourceToSave+=10;
      StringBuffer response = requestSOLR(url+cursorMark);
      JsonArray results = parserJson.parse(response.toString()).getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray();
      count+=10;
      if(cursorMark.equals("AoEpOTk5OTVfMTAy")){
        System.out.println(results);
        break;
      }

      for(int i=0; i<results.size();i++){
        JsonElement sourceElement = results.get(i);
        JsonObject sourceObject = sourceElement.getAsJsonObject();
        String description;
        if(sourceObject!=null && sourceObject.get("dc_description")!=null){
          description = sourceObject.get("dc_description").getAsString();
          String title = sourceObject.get("dc_title").getAsString();
          String id = sourceObject.get("id").getAsString();
          System.out.println(id);
          Source source = new Source();
          source.setTitle(title);
          source.setId(id);
          List<String> toAnalyze = new ArrayList<String>();
          toAnalyze.add(source.getTitle());
          toAnalyze.add(description);
          source.setKeywordList((ArrayList<Keyword>)extractorInnen.extractKeywordsFromText(toAnalyze,4));
          sourceList.add(source); 
        }
      }
      cursorMark = parserJson.parse(response.toString()).getAsJsonObject().get("nextCursorMark").getAsString();

    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.trainingAndTestFolder+"dataSourcesWithoutCategory.json"), sourceList);

    System.out.println(count);
  }
  
  
  public List<Source> getSourcesFromSolr(List<String> idPapers, Class c) throws IOException{

    List<Source> toReturn = new ArrayList<Source>();
    Gson gson = new Gson();
    JsonArray resultsProduzione = new JsonArray();
    JsonArray resultsLocal = new JsonArray();
    JsonParser parserJson = new JsonParser();

    for(String id : idPapers){
      if(Paper.class.isAssignableFrom(c)){

        /*
        String querylocale = "http://localhost:8983/solr/technical_papers/select?q=id%3A"+id+"&fl=id%2Cdc_title%2Cdc_description&wt=json&indent=true";
        StringBuffer responseLocale = requestSOLR(querylocale);;
        resultsLocal.add(parserJson.parse(responseLocale.toString()).getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray());
         */

        String queryProduzione = "http://192.168.200.81:8080/solr4/technical_papers/select?q=id%3A"+id+"&fl=id%2Cdc_title%2Cdc_description&wt=json&indent=true";
        StringBuffer responseProduzione = requestSOLR(queryProduzione);
        if(responseProduzione != null)
          resultsProduzione.add(parserJson.parse(responseProduzione.toString()).getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray());


      }else if(Patent.class.isAssignableFrom(c)){
        //nuova query per i patent
      }
    }

    for(JsonElement json: resultsProduzione){
      String tmpJson = json.toString().replace("[", "").replaceAll("]", "");
      Paper paper = gson.fromJson(tmpJson, Paper.class); 
      if(paper!=null){
        toReturn.add(paper.getSource());
      }
    }
    return toReturn;
  }




  private static StringBuffer requestSOLR(String url) throws IOException{
    final String USER_AGENT = "Mozilla/5.0";

    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setDoOutput(true);
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", USER_AGENT);

    BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    return response;
  }


}

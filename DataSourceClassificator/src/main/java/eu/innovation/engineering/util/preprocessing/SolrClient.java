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

import eu.innovation.engineering.config.Configurator;
import eu.innovation.engineering.config.PathConfigurator;
import eu.innovation.engineering.keyword.extractor.innen.InnenExtractor;
import eu.innovation.engineering.keyword.extractor.interfaces.KeywordExtractor;

/**
 * Questa classe serve per generare i dataset Json di test, facendo query da Solr per prendere N source
 * @author lomasto
 *
 */
public class SolrClient {

  private static final String remoteAddress = "http://192.168.200.82:8080/solr4";


  public static void main(String[] args) throws Exception{

    KeywordExtractor ke = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    requestNTechincalPaper(0,10000,ke);

  }


  public static void useManualCheckKeywords(String id) throws Exception{
    SolrClient cl = new SolrClient();
    System.out.println(cl.checkKeywords(id));
  }


  public List<String> checkKeywords(String id) throws Exception{
    List<String> toReturn = new ArrayList<>();
    toReturn.add(id);
    List<Source> sources = getSourcesFromSolr(toReturn, Paper.class);

    List<String> texts = new ArrayList<>();
    for(Source s:sources){
      System.out.println(s.getTexts());
      texts.addAll(s.getTexts());
    }


    toReturn.remove(id);
    KeywordExtractor innenK = new InnenExtractor(PathConfigurator.keywordExtractorsFolder);
    innenK.extractKeywordsFromTexts(texts, 4).stream().flatMap(l -> l.stream()).map(Keyword::getText).forEach(toReturn::add);

    return toReturn;
  }


  public static void requestNPatent(int firstPatentToJump,int numSourceRequest,KeywordExtractor ke) throws Exception{

    String cursorMark="*";

    String url = "http://192.168.200.81:8080/solr4/patents/select?q=original_language%3A+%22eng%22+AND%0Aabstract+%3A+%5B%22%22+TO+*%5D%0A&sort=id+asc&fl=id%2Cabstract%2Cinvention_title_en%2Coriginal_language&wt=json&indent=true&cursorMark=";

    int numSourceToSave = 0;
    JsonParser parserJson = new JsonParser();

    //creo il file 
    int count = 0;
    ArrayList<Source> sourceList = new ArrayList<Source>();

    //Salto i primi paper
    int paperJumped =0;
    while(paperJumped<firstPatentToJump){
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
        if(sourceObject!=null && sourceObject.get("abstract")!=null && (!sourceObject.get("abstract").getAsString().equals(""))){
          description = sourceObject.get("abstract").getAsString();
          String title = sourceObject.get("invention_title_en").getAsString();
          String id = sourceObject.get("id").getAsString();
          System.out.println(id);
          Source source = new Source();
          source.setTitle(title);
          source.setId(id);
          List<String> toAnalyze = new ArrayList<String>();
          toAnalyze.add(source.getTitle()+description); 
          if(ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords)!=null && ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).size()>0)
            if(ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0)!=null)
              if(ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0).size()>0){
                ArrayList<Keyword> keywordsList = (ArrayList<Keyword>) ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0);
                source.setKeywordList(keywordsList);
                sourceList.add(source); 
              }
        }
      }
      cursorMark = parserJson.parse(response.toString()).getAsJsonObject().get("nextCursorMark").getAsString();

    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.trainingAndTestFolder+"dataSourcesForTest.json"), sourceList);

    System.out.println(count);
  }



  public static void requestNTechincalPaper(int firstPaperToJump,int numSourceRequest, KeywordExtractor ke) throws Exception{

    String cursorMark="*";
    String url = "http://192.168.200.81:8080/solr4/technical_papers/select?q=*%3A*&sort=id+asc&fl=id%2Cdc_title%2Cdc_description&wt=json&indent=true&cursorMark=";

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
          toAnalyze.add(source.getTitle()+description);
          try{
          if(ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords)!=null && ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).size()>0)
            if(ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0)!=null && !ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0).isEmpty()){
              if(ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0).size()>0){
                ArrayList<Keyword> keywordsList = (ArrayList<Keyword>) ke.extractKeywordsFromTexts(toAnalyze, Configurator.numKeywords).get(0);
                source.setKeywordList(keywordsList);
                sourceList.add(source); 
                }
              }
            }
          catch(Exception ex){
            System.out.println("Vado in exception per un motivo sconosciuto");
          }
        }
      }
      cursorMark = parserJson.parse(response.toString()).getAsJsonObject().get("nextCursorMark").getAsString();

    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PathConfigurator.trainingAndTestFolder+"test.json"), sourceList);

    System.out.println(count);
  }


  private List<String> balanceQuery(List<String> idPapers){

    String idsString = "";
    List<String> toReturn = new ArrayList<>();

    for(int i = 0;i<idPapers.size();i++){
      if(i==idPapers.size()-1)
        idsString+=idPapers.get(i);
      else
        idsString+=idPapers.get(i)+",";
      if(i%Configurator.solrQueryLimit == 0 && i != 0){
        toReturn.add(idsString);
        idsString = "";
      }
    }
    if(!idsString.equals("")){
      toReturn.add(idsString);
    }
    return toReturn;
  }


  public List<Source> getSourcesFromSolr(List<String> list, Class c) throws IOException{

    List<Source> toReturn = new ArrayList<Source>();
    Gson gson = new Gson();
    JsonArray resultsProduzione = new JsonArray();
    JsonParser parserJson = new JsonParser();

    List<String> idSources = balanceQuery(list);

    for(String id : idSources){

      if(Paper.class.isAssignableFrom(c)){
        String queryProduzione = remoteAddress+"/technical_papers/get?ids="+id+"&fl=id,dc_title,dc_description,full_text";
        StringBuffer responseProduzione = requestSOLR(queryProduzione);
        if(responseProduzione != null){
          resultsProduzione.add(parserJson.parse(responseProduzione.toString()).getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray());
        }

      }else if(Patent.class.isAssignableFrom(c)){
        //nuova query per i patent
      }
    }
    for(JsonElement json: resultsProduzione){
      JsonArray tmpJson = json.getAsJsonArray();
      for(JsonElement el: tmpJson){
        Paper paper = gson.fromJson(el, Paper.class); 
        if(paper!=null){
          toReturn.add(paper.getSource());
        }
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

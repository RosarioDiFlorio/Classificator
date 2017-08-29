package eu.innovation.engineering.util.preprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class SolrClient {


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




  private StringBuffer requestSOLR(String url) throws IOException{
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

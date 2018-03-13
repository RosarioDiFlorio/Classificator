package eu.innovationengineering.utilities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HttpRequester {
  
  /**
   * @param targetURL
   * @return
   * @throws IOException
   */
  public static  JsonObject getJsonResponse(String targetURL) throws IOException{
    URL url = new URL(targetURL);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setDoOutput(true);
    con.setRequestMethod("GET");
    JsonObject jOb = new JsonObject();
    try{
      Scanner in = new Scanner(new InputStreamReader(con.getInputStream()));  
      JsonParser parser = new JsonParser(); 
      jOb = parser.parse(in.nextLine()).getAsJsonObject();
    }
    catch(ConnectException e){
      System.out.println("Connection timed out: recall method ");
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      jOb = getJsonResponse(targetURL);
    }
    return jOb;
  }

}

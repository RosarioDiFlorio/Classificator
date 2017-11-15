package eu.innovation.engineering.wikipedia;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;



/**
 * @author Rosario
 *
 */
@WebServlet("/")
public class ReaderRequest extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response){
    JsonParser parser  = new JsonParser();
    JsonArray categoriesJson  = parser.parse(request.getParameter("categories")).getAsJsonArray();
   
    Set<String> categories = new HashSet<String>();
    for(int i = 0;i<categoriesJson.size();i++){
      categories.add(categoriesJson.get(i).getAsString());
    }
    
    try {
      WikipediaMiner.buildDataset("volume/dataset", categories, new HashSet<String>(), 0, true);
    }
    catch (InterruptedException | ExecutionException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response){
    JsonParser parser  = new JsonParser();
    JsonArray categoriesJson  = parser.parse(request.getParameter("categories")).getAsJsonArray();
   
    Set<String> categories = new HashSet<String>();
    for(int i = 0;i<categoriesJson.size();i++){
      categories.add(categoriesJson.get(i).getAsString());
    }
    
    try {
      WikipediaMiner.buildDataset("volume/dataset", categories, new HashSet<String>(), 0, true);
    }
    catch (InterruptedException | ExecutionException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}

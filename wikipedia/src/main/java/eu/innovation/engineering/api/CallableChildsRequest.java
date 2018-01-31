package eu.innovation.engineering.api;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.innovation.engineering.graph.main.CrawlerGraphWikipedia;



public class CallableChildsRequest implements Callable<Map<String,Set<String>>> {

	private String category;

	public CallableChildsRequest(String category){
		this.category=category;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}


	@Override
  public Map<String, Set<String>> call() throws Exception {

		Map<String, Set<String>> toReturn = new HashMap<String, Set<String>>();

		String categoryEncoded = URLEncoder.encode(this.category,"utf-8");
		String childsURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+categoryEncoded+"&cmlimit=500&cmtype=subcat&format=json";

		// make 2 request
		CrawlerGraphWikipedia crawler = new CrawlerGraphWikipedia();
		JsonObject responseChild = WikipediaAPI.getJsonResponse(childsURL);


		// second categoryList to obtain childs. Check if current category has parent with try-catch.

		JsonArray categoriesChilds = null;
		try{
			categoriesChilds = responseChild.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
		}		
		catch(Exception e){
			System.out.println(this.category+": hasn't childs category --- URL: "+childsURL);
		}

		Set<String> categoriesChildToReturn = new HashSet<String>();


		if(categoriesChilds!=null){
			// add all certex obtained to hashset
			for(JsonElement cat : categoriesChilds){
				String name = cat.getAsJsonObject().get("title").getAsString();
				String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
				categoriesChildToReturn.add(namesplitted[1]);
			}
			toReturn.put(this.category.replace(" ", "_"), categoriesChildToReturn);
		}
		
		return toReturn;
	}




}

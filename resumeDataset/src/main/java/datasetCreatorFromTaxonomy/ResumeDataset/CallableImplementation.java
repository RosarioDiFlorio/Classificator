package datasetCreatorFromTaxonomy.ResumeDataset;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CallableImplementation implements Callable<HashMap<String,HashSet<String>>> {

	private String category;

	public CallableImplementation(String category){
		this.category=category;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}


	public HashMap<String, HashSet<String>> call() throws Exception {

		HashMap<String, HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();

		String categoryEncoded = URLEncoder.encode(this.category,"utf-8");
		String childsURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+categoryEncoded+"&cmlimit=500&cmtype=subcat&format=json";

		// make 2 request
		CrawlerWikipediaCategory crawler = new CrawlerWikipediaCategory();
		JsonObject responseChild = crawler.getJsonResponse(childsURL);


		// second categoryList to obtain childs. Check if current category has parent with try-catch.

		JsonArray categoriesChilds = null;
		try{
			categoriesChilds = responseChild.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
		}		
		catch(Exception e){
			System.out.println(this.category+": hasn't childs category --- URL: "+childsURL);
		}

		HashSet<String> categoriesChildToReturn = new HashSet<String>();


		if(categoriesChilds!=null){
			// add all certex obtained to hashset
			for(JsonElement cat : categoriesChilds){
				String name = cat.getAsJsonObject().get("title").getAsString();
				String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
				categoriesChildToReturn.add(namesplitted[1]);
			}
			toReturn.put(this.category, categoriesChildToReturn);
		}
		
		return toReturn;
	}




}

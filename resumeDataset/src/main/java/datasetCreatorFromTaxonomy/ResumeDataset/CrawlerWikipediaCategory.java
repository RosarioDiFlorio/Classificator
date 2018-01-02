package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CrawlerWikipediaCategory {




	public static void main(String[] args) throws IOException{

		BFS("Contents");

	}

	public static Category BFS(String category) throws IOException{
		HashSet<String> markedNode = new HashSet<String>();
		markedNode.add(category);
		//Primo nodo
		PriorityQueue<String> vertexToVisit = new PriorityQueue<String>();
		vertexToVisit.add(category);
		Category graph = new Category(category,wikipediaRequest(category));
		String u = vertexToVisit.poll();
		HashMap<String, Category> subCategory = graph.getChilds();
		for(String v  : subCategory.keySet()) {
			if(!markedNode.contains(v)){
				vertexToVisit.add(v);
				markedNode.add(v);
			}
		}
		//Tutti gli altri nodi
		while(!vertexToVisit.isEmpty()){
			String z = vertexToVisit.poll();
			Category vertex = new Category(z,wikipediaRequest(z));
			subCategory = vertex.getChilds();
			for(String v  : subCategory.keySet()) {
				if(!markedNode.contains(v)){
					vertexToVisit.add(v);
					vertex.addChild(subCategory.get(v));
					markedNode.add(v);
				}
			}
		}

		System.out.println("TotalCategory: "+markedNode.size());
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		System.out.println("Ora di fine :"+dateFormat.format(date));
		return graph;
	}


	public static HashMap<String, Category> wikipediaRequest(String category) throws IOException{
		String targetURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+category+"&cmtype=subcat&format=json";
		System.out.println(category);
		JsonObject response = getJsonResponse(targetURL);
		JsonArray categories = response.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
		HashMap<String,Category> categoriesToReturn = new HashMap<String, Category>();
		for(JsonElement cat : categories){
			String name = cat.getAsJsonObject().get("title").getAsString();
			String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
			categoriesToReturn.put(namesplitted[1], new Category(namesplitted[1], new HashMap<String,Category>()));
		}
		//System.out.println(category+" "+categoriesToReturn.keySet());
		return categoriesToReturn;

	}



	private static  JsonObject getJsonResponse(String targetURL) throws IOException{
		final String USER_AGENT = "Mozilla/5.0";
		URL url = new URL(targetURL);
		//System.out.println(url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		Scanner in = new Scanner(
				new InputStreamReader(con.getInputStream()));  
		JsonParser parser = new JsonParser(); 
		JsonObject jOb = parser.parse(in.nextLine()).getAsJsonObject();
		return jOb;
	}
}

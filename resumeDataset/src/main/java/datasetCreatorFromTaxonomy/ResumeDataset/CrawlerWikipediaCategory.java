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

	public static HashMap BFS(String category) throws IOException{
		// Adjacency List
		HashMap<String,AdjacencyListRow> adjacencylist = new HashMap<>();
		
		// contains marked nodes already visited
		HashSet<String> markedNode = new HashSet<String>();
		
		// add first category
		markedNode.add(category);
		
		// Queue vertex to visit 
		PriorityQueue<String> vertexToVisit = new PriorityQueue<String>();
		vertexToVisit.add(category);
		
		// while there are vertex to visit, build adyacency list
		while(!vertexToVisit.isEmpty()){
			String vertex = vertexToVisit.poll();
			HashSet<String> linkedVertex = wikipediaRequest(vertex);
			AdjacencyListRow currentVertex = new AdjacencyListRow(linkedVertex, false);
			adjacencylist.put(vertex, currentVertex);
			for(String v : linkedVertex){
				if(!markedNode.contains(v)){
					vertexToVisit.add(v);
					markedNode.add(v);
				}
			}
		}

		System.out.println("TotalCategory: "+markedNode.size());
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		System.out.println("Ora di fine :"+dateFormat.format(date));
		return adjacencylist;
	}


	/**
	 * Return vertexList linked from current vertex. In this method are used two request WIKIPEDIA to obtain parents list and childs list.
	 * @param category
	 * @return
	 * @throws IOException
	 */
	public static HashSet<String> wikipediaRequest(String category) throws IOException{
		//define 2 wikipedia request
		String childsURL = "https://en.wikipedia.org/w/api.php?action=query&titles=Category:"+category+"&prop=categories&clshow=!hidden&cllimit=500&indexpageids&format=json";
		String parentsURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+category+"&cmlimit=500&cmtype=subcat&format=json";

		// make 2 request
		JsonObject responseParent = getJsonResponse(parentsURL);
		JsonObject responseChild = getJsonResponse(childsURL);

		// first categoryList to obtain childs
		JsonArray categories = responseParent.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
		// second categoryList to obtain parents. Check if current category has parent with try-catch.
		String id = responseChild.get("query").getAsJsonObject().get("pageids").getAsJsonArray().get(0).getAsString();
		try{
			categories.addAll(responseChild.get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray());
		}		
		catch(Exception e){
			System.out.println(id+": hasn't parent category");
		}
		
		HashSet<String> categoriesToReturn = new HashSet<>();
		
		// add all certex obtained to hashset
		for(JsonElement cat : categories){
			String name = cat.getAsJsonObject().get("title").getAsString();
			String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
			categoriesToReturn.add(namesplitted[1]);
		}

		System.out.println(category+" "+categoriesToReturn.size());

		return categoriesToReturn;

	}



	/**
	 * this method is used to do wikipedia request
	 * @param targetURL
	 * @return JsonObject response
	 * @throws IOException
	 */
	private static  JsonObject getJsonResponse(String targetURL) throws IOException{
		final String USER_AGENT = "Mozilla/5.0";
		URL url = new URL(targetURL);
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

package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CrawlerWikipediaCategory {




	public static void main(String[] args) throws IOException{

		BackupBFS("Contents", true);

	}

	public static void BackupBFS(String category,boolean persist) throws JsonParseException, JsonMappingException, IOException{
		CrawlerResult crawlerResult = new CrawlerResult(false,category,new HashSet<String>(),new HashMap<String,AdjacencyListRow>(),new PriorityQueue<String>());
		File crawlerResultFile = new File(crawlerResult.getClass().getSimpleName());

		if(crawlerResultFile.exists()){
			ObjectMapper mapper = new ObjectMapper();
			crawlerResult = mapper.readValue(crawlerResultFile, new TypeReference<CrawlerResult>() {});
		}
		try{
			BFS(crawlerResult.getLatestCategoryProcessed(), crawlerResult.getMarkedNode(), crawlerResult.getAdjacencyList(),crawlerResult.getVertexToVisit(), persist);

		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println("Something went wrong");
			crawlerResult.setCrashed(true);

		}
	}



	/**
	 * @param category
	 * @param markedNode contains marked nodes already visited
	 * @param adjacencylist Adjacency List
	 * @return
	 * @throws IOException
	 */
	public static HashMap BFS(String category,HashSet<String> markedNode, HashMap<String,AdjacencyListRow> adjacencylist,PriorityQueue<String> vertexToVisit,boolean persist) throws IOException{

		// add first category
		markedNode.add(category);

		// Queue vertex to visit 
		vertexToVisit.add(category);

		// while there are vertex to visit, build adyacency list
		while(!vertexToVisit.isEmpty()){
			String vertex = vertexToVisit.poll();
			markedNode.add(vertex);
			HashMap<String, HashSet<String>> linkedVertex = wikipediaRequest(vertex);

			AdjacencyListRow currentVertex = new AdjacencyListRow(linkedVertex.get("parents"), false);
			adjacencylist.put(vertex, currentVertex);
			if(persist){
				CrawlerResult crawlerResult = new CrawlerResult(false,vertex,markedNode, adjacencylist,vertexToVisit);
				ObjectMapper writerCrawlerResult = new ObjectMapper();
				writerCrawlerResult.writerWithDefaultPrettyPrinter().writeValue(new File(crawlerResult.getClass().getSimpleName()), crawlerResult);
			}
			HashSet<String> app = new HashSet<String>();
			app.addAll(linkedVertex.get("parents"));
			app.addAll(linkedVertex.get("childs"));
			for(String v : app){
				if(!markedNode.contains(v) && !vertexToVisit.contains(v)){
					vertexToVisit.add(v);
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
	public static HashMap<String,HashSet<String>> wikipediaRequest(String category) throws IOException{
		//define 2 wikipedia request
		HashMap<String, HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();
		String parentsURL = "https://en.wikipedia.org/w/api.php?action=query&titles=Category:"+category+"&prop=categories&clshow=!hidden&cllimit=500&indexpageids&format=json";
		String childsURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+category+"&cmlimit=500&cmtype=subcat&format=json";




		// make 2 request
		JsonObject responseParent = getJsonResponse(parentsURL);
		JsonObject responseChild = getJsonResponse(childsURL);

		JsonArray categoriesParent = null;
		// first categoryList to obtain parents
		String id = responseParent.get("query").getAsJsonObject().get("pageids").getAsJsonArray().get(0).getAsString();
		try{
			categoriesParent = responseParent.getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray();
		}
		catch(Exception e){
			System.out.println(id+": hasn't parents category");
		}
		// second categoryList to obtain childs. Check if current category has parent with try-catch.
		
		JsonArray categoriesChilds = null;
		try{
			categoriesChilds = responseChild.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
		}		
		catch(Exception e){
			System.out.println(id+": hasn't childs category");
		}

		HashSet<String> categoriesParentToReturn = new HashSet<String>();
		HashSet<String> categoriesChildToReturn = new HashSet<String>();

		if(categoriesParent!=null){
			// add all certex obtained to hashset
			for(JsonElement cat : categoriesParent){
				String name = cat.getAsJsonObject().get("title").getAsString();
				String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
				categoriesParentToReturn.add(namesplitted[1]);
			}
		}

		if(categoriesChilds!=null){
			// add all certex obtained to hashset
			for(JsonElement cat : categoriesChilds){
				String name = cat.getAsJsonObject().get("title").getAsString();
				String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
				categoriesChildToReturn.add(namesplitted[1]);
			}
		}

		toReturn.put("parents", categoriesParentToReturn);
		toReturn.put("childs", categoriesChildToReturn);


		return toReturn;

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

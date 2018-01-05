package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

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
		
		HashSet<String> categories = new HashSet<String>(); 
		categories.add("Contents");
		BackupBFS(categories, true);

	}

	public static HashMap<String, HashSet<String>> getParentsRequest(HashSet<String> categories) throws IOException{



		// key is categories name, value is list of parent category
		HashMap<String,HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();

		JsonArray categoriesParent = null;
		// first categoryList to obtain parents

		String categoryList = "";
		for(String s : categories){
			categoryList+="Category:"+s.replace(" ", "_")+"|";
		}

		String parentsURL = "https://en.wikipedia.org/w/api.php?action=query&titles="+categoryList+"&prop=categories&clshow=!hidden&cllimit=500&indexpageids&format=json";
		JsonObject responseParent = getJsonResponse(parentsURL);

		//build ids array 
		JsonArray idsJsonArray = responseParent.get("query").getAsJsonObject().get("pageids").getAsJsonArray();
		ArrayList<String> ids = new ArrayList<String>();
		for (JsonElement e : idsJsonArray){

			if (Integer.parseInt(e.getAsString())>0){
				ids.add(e.getAsString());
			}
		}


		for(String id : ids){
			HashSet<String> currentParentCategory = new HashSet<String>();
			try{
				categoriesParent = responseParent.getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("categories").getAsJsonArray();
				String title = responseParent.getAsJsonObject().get("query").getAsJsonObject().get("pages").getAsJsonObject().get(id).getAsJsonObject().get("title").getAsString();
				if(categoriesParent!=null){
					// add all certex obtained to hashset
					for(JsonElement cat : categoriesParent){
						String name = cat.getAsJsonObject().get("title").getAsString();
						String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
						currentParentCategory.add(namesplitted[1]);
					}

					toReturn.put(title.replace("Category:", ""), currentParentCategory);
				}
			}
			catch(Exception e){
				System.out.println(id+": hasn't parents category --- URL: "+parentsURL);
			}

		}


		return toReturn;

	}



	public static HashMap<String, HashSet<String>> getChildsRequest(HashSet<String> categories) throws IOException{
		HashMap<String, HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();


		for(String category : categories){
			String categoryEncoded = URLEncoder.encode(category,"utf-8");
			String childsURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+categoryEncoded+"&cmlimit=500&cmtype=subcat&format=json";

			// make 2 request
			JsonObject responseChild = getJsonResponse(childsURL);


			// second categoryList to obtain childs. Check if current category has parent with try-catch.

			JsonArray categoriesChilds = null;
			try{
				categoriesChilds = responseChild.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
			}		
			catch(Exception e){
				System.out.println(category+": hasn't childs category --- URL: "+childsURL);
			}

			HashSet<String> categoriesChildToReturn = new HashSet<String>();


			if(categoriesChilds!=null){
				// add all certex obtained to hashset
				for(JsonElement cat : categoriesChilds){
					String name = cat.getAsJsonObject().get("title").getAsString();
					String [] namesplitted = name.replaceAll(" ", "_").split("Category:");
					categoriesChildToReturn.add(namesplitted[1]);
				}
				toReturn.put(category, categoriesChildToReturn);
			}
		}
		return toReturn;
	}






	public static void BackupBFS(Set<String> categories,boolean persist) throws JsonParseException, JsonMappingException, IOException{
		CrawlerResult crawlerResult = new CrawlerResult(false,categories,new HashSet<String>(),new HashMap<String,AdjacencyListRow>(),new PriorityQueue<String>());
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
	public static HashMap BFS(Set<String> categoryList,HashSet<String> markedNode, HashMap<String,AdjacencyListRow> adjacencylist,PriorityQueue<String> vertexToVisit,boolean persist) throws IOException{

		// add first category
		markedNode.addAll(categoryList);

		// Queue vertex to visit 
		vertexToVisit.addAll(categoryList);
		int countToPersiste = 0;
		int countToAddQuery = 0;
		// while there are vertex to visit, build adyacency list
		while(!vertexToVisit.isEmpty()){
			HashSet<String> categories = new HashSet<String>(categoryList);
			
			//NEW//
			while (countToAddQuery < 15 && (!vertexToVisit.isEmpty()) && categories.size()<10){
				categories.add(vertexToVisit.poll());
				countToAddQuery++;
				countToPersiste++;
				if(vertexToVisit.isEmpty())
					break;
			}
			
			countToAddQuery = 0;
			
			//
			
			//String vertex = vertexToVisit.poll();
			//count++;
			//markedNode.add(vertex);
			//HashMap<String, HashSet<String>> linkedVertex = wikipediaRequest(vertex);
			
			//NEW
			markedNode.addAll(categories);
			HashMap<String, HashMap<String, HashSet<String>>> linkedVertex = wikipediaRequest(categories);
			HashMap<String, HashSet<String>> parent = linkedVertex.get("parents");
			
			for(String e : parent.keySet()){
				AdjacencyListRow currentVertex = new AdjacencyListRow(parent.get(e), false);
				adjacencylist.put(e, currentVertex);
			}
			
			if((persist && countToPersiste%1000==0) || (vertexToVisit.isEmpty())){
				System.out.println("NODI RIMANENTI: "+vertexToVisit.size());
				System.out.println("NODI MARCATI: "+markedNode.size());
				System.out.println("------------------------------------------------------");
				CrawlerResult crawlerResult = new CrawlerResult(false,parent.keySet(),markedNode, adjacencylist,vertexToVisit);
				ObjectMapper writerCrawlerResult = new ObjectMapper();
				writerCrawlerResult.writerWithDefaultPrettyPrinter().writeValue(new File(crawlerResult.getClass().getSimpleName()), crawlerResult);
			}
			
			
			HashSet<String> app = new HashSet<String>();
			
			HashMap<String, HashSet<String>> parentsMap = linkedVertex.get("parents");
			HashMap<String, HashSet<String>> childsMap = linkedVertex.get("childs");
			for(String key : parentsMap.keySet()){
				app.addAll(parentsMap.get(key));
			}
			for(String key : childsMap.keySet()){
				app.addAll(childsMap.get(key));
			}
	
			for(String v : app){
				if(!markedNode.contains(v) && !vertexToVisit.contains(v)){
					vertexToVisit.add(v);
				}
			}
			
			categoryList = new HashSet<String>();
			//

			//AdjacencyListRow currentVertex = new AdjacencyListRow(linkedVertex.get("parents"), false);
			//adjacencylist.put(vertex, currentVertex);
			
			/*if((persist && count%1000==0) || (vertexToVisit.isEmpty())){
				System.out.println(markedNode.size());
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
			}*/
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
	 * @return
	 * @throws IOException
	 */
	/*public static HashMap<String,HashSet<String>> wikipediaRequest(String category) throws IOException{
		//define 2 wikipedia request
		HashMap<String, HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();
		String categoryEncoded = URLEncoder.encode(category,"utf-8");
		String parentsURL = "https://en.wikipedia.org/w/api.php?action=query&titles=Category:"+categoryEncoded+"&prop=categories&clshow=!hidden&cllimit=500&indexpageids&format=json";
		String childsURL="https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmtitle=Category:"+categoryEncoded+"&cmlimit=500&cmtype=subcat&format=json";




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
			System.out.println(id+": hasn't parents category --- URL: "+parentsURL);
		}
		// second categoryList to obtain childs. Check if current category has parent with try-catch.

		JsonArray categoriesChilds = null;
		try{
			categoriesChilds = responseChild.getAsJsonObject().get("query").getAsJsonObject().get("categorymembers").getAsJsonArray();
		}		
		catch(Exception e){
			System.out.println(id+": hasn't childs category --- URL: "+childsURL);
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

	} */

	public static HashMap<String,HashMap<String, HashSet<String>>> wikipediaRequest(HashSet<String> categories) throws IOException{
		
		HashMap<String, HashMap<String, HashSet<String>>> toReturn = new HashMap<String, HashMap<String,HashSet<String>>>();
		toReturn.put("parents", getParentsRequest(categories));
		toReturn.put("childs", getChildsRequest(categories));
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

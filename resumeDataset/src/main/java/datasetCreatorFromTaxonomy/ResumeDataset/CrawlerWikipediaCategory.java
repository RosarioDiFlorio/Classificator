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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CrawlerWikipediaCategory {


	private static int numConcurrency = 20;
	private static 	ExecutorService executorService = Executors.newFixedThreadPool(numConcurrency);



	public static void main(String[] args) throws IOException{
		try {
			HashSet<String> categories = new HashSet<String>(); 
			categories.add("Contents");
			BackupBFS(categories, true);
		}
		finally {
			executorService.shutdown();
		}

	}

	/**
	 * This method is used to do request to obtain parent category
	 * @param categories. Category list, used to build request with more category. For any category is returned a list of parent category
	 * @return HashMap<String, HashSet<String>>, keys are names of initial categories. HashSet are parent category for any initial category
	 * @throws IOException
	 */
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
					// add all vertex obtained to hashset
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


	/**
	 * This method is used to do request to obtain child category
	 * @param categories. Category list, used to build request with more category. For any category is returned a list of child category
	 * @return HashMap<String, HashSet<String>>, keys are names of initial categories. HashSet are child category for any initial category
	 * @throws IOException
	 */
	public static HashMap<String, HashSet<String>> getChildsRequest(HashSet<String> categories) throws IOException, InterruptedException, ExecutionException{
		HashMap<String, HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();


		ArrayList<Future> featureList = new ArrayList<Future>();
		for(String category : categories){
			CallableImplementation currentCallable = new CallableImplementation(category);
			featureList.add(executorService.submit(currentCallable));
		}

		for ( Future future : featureList) {
			HashMap<String, HashSet<String>> childrenMap = (HashMap<String, HashSet<String>>) future.get();
			for(String key : childrenMap.keySet()){
				toReturn.put(key, childrenMap.get(key));
			}
		}
		return toReturn;
	}





	/**
	 * Used to read graph by backup and to call BFS methods
	 * @param categories
	 * @param persist
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
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
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static HashMap BFS(Set<String> categoryList,HashSet<String> markedNode, HashMap<String,AdjacencyListRow> adjacencylist,PriorityQueue<String> vertexToVisit,boolean persist) throws IOException, InterruptedException, ExecutionException{

		// add first category
		markedNode.addAll(categoryList);

		// Queue vertex to visit 
		vertexToVisit.addAll(categoryList);
		int countToPersiste = 0;
		int countToAddQuery = 0;
		// while there are vertex to visit, build adyacency list
		while(!vertexToVisit.isEmpty()){
			HashSet<String> categories = new HashSet<String>(categoryList);

			while (countToAddQuery < numConcurrency && (!vertexToVisit.isEmpty()) && categories.size()< numConcurrency){
				categories.add(vertexToVisit.poll());
				countToAddQuery++;
				countToPersiste++;
				if(vertexToVisit.isEmpty())
					break;
			}

			countToAddQuery = 0;

			markedNode.addAll(categories);
			HashMap<String, HashMap<String, HashSet<String>>> linkedVertex = wikipediaRequest(categories);
			HashMap<String, HashSet<String>> parent = linkedVertex.get("parents");

			for(String e : parent.keySet()){
				AdjacencyListRow currentVertex = new AdjacencyListRow(parent.get(e), false);
				adjacencylist.put(e, currentVertex);
			}

			if((persist && countToPersiste == (numConcurrency*10)) || (vertexToVisit.isEmpty())){
				countToPersiste = 0;
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
			
		}

		System.out.println("TotalCategory: "+markedNode.size());
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		System.out.println("Ora di fine :"+dateFormat.format(date));
		return adjacencylist;
	}


	

	/**
	 * Method used to do wikipedia request
	 * @param categories
	 * @return response that contains parent and child for any category contained into initial list
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static HashMap<String,HashMap<String, HashSet<String>>> wikipediaRequest(HashSet<String> categories) throws IOException, InterruptedException, ExecutionException{

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
	public static  JsonObject getJsonResponse(String targetURL) throws IOException{
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

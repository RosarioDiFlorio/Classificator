package datasetCreatorFromTaxonomy.ResumeDataset;

import java.beans.VetoableChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonGenerationException;
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

	/**
	 * used to build wikipedia category graph
	 * @param args
	 * @throws IOException
	 */
	public static void mainToBuildGraph(String[] args) throws IOException{
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
	 * used to mark category
	 * @param args
	 * @throws IOException
	 */
	public static void mainToMarkGraph(String[] args) throws IOException{
		HashMap<String, AdjacencyListRow> graph = returnAdjacencyListFromFile("GraphWikipedia");
		Set<String> categories =  returnCategoriesFromTaxonomyCSV("categories_taxonomy.csv");
		markGraph(graph, categories);
	}

	
	/**
	 * main to create weighed graph
	 * @param args
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static void main(String args[]) throws JsonParseException, JsonMappingException, IOException{
		HashMap<String, AdjacencyListRow> adjacencyList = CrawlerWikipediaCategory.returnAdjacencyListFromFile("signedGraphWikipedia");
		Map<String, float[]> vectorsFromWikipediaGraph = AnalyzerWikipediaGraph.loadVectorsWikipediaGraph("vectorsWikipediaVertex");
		Map<String, AdjacencyListRowVertex> result = fromAdjacencyListRowToAdjacencyListRowVertex(vectorsFromWikipediaGraph,adjacencyList);
		ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter().writeValue(new File("graphWikipediaWeighed"), result);
		
	}
	

	/**
	 * this method check if a vector is different from 0 vector
	 * @param vector
	 * @return
	 */
	public static boolean validateVector(float[] vector){

		if (vector == null)
			return false;
		
		for(int i=0;i<vector.length-1;i++){
			if (vector[i]!=0.0)
				return true;
		}
		return false;
	}
	
	
	
	/**
	 * read vectors object 
	 * @param vectorsFromWikipediaGraph
	 * @param adjacencyList
	 * @return
	 */
	public static Map<String,AdjacencyListRowVertex> fromAdjacencyListRowToAdjacencyListRowVertex(Map<String,float[]> vectorsFromWikipediaGraph,Map<String,AdjacencyListRow> adjacencyList){
		Map<String,AdjacencyListRowVertex> toReturn = new HashMap<>();

		for(String key : adjacencyList.keySet()){
			AdjacencyListRowVertex adjacencyListRowVertex = new AdjacencyListRowVertex();
			adjacencyListRowVertex.setTaxonomyCategory(adjacencyList.get(key).isTaxonomyCategory());
			float[] keyVector = vectorsFromWikipediaGraph.get(key);
			
			if(keyVector== null){
				System.out.println("key null: "+key);
			}
			else{
				System.out.println("key not null: "+key);
			}
			
			for(String parent : adjacencyList.get(key).getLinkedVertex()){
				Vertex currentParent = new Vertex();
				currentParent.setVertexName(parent);
				float[] parentVector = vectorsFromWikipediaGraph.get(parent);
				
				if(parentVector== null)
					System.out.println("parent null : "+parent);
				else
					System.out.println("parent not null : "+parent);
				
				if(validateVector(keyVector) && validateVector(parentVector)){
					
					currentParent.setSimilarity(cosineSimilarityInverse(keyVector, parentVector));
				}
				else{
					currentParent.setSimilarity(0.5);
				}
				adjacencyListRowVertex.getLinkedVertex().add(currentParent);
			}
			toReturn.put(key, adjacencyListRowVertex);
		}
		return toReturn;
	}




	/**
	 * main uset to clean graph   categoryName: "category name" --> "category_name"
	 * @param args
	 * @throws IOException
	 */
	public static void mainToClear(String[] args) throws IOException{
		HashMap<String, AdjacencyListRow> graph = returnAdjacencyListFromFile("CrawlerResult");
		HashMap<String, AdjacencyListRow> app = new HashMap<String, AdjacencyListRow>();

		System.out.println("Initial graph size: "+graph.size());

		HashSet<String> toRemove = new HashSet<String>();
		for(String category : graph.keySet()){

			if(category.contains(" ")){
				app.put(category.replace(" ", "_"),graph.get(category));
				toRemove.add(category);
			}
		}

		//aggiungo le categorie con _
		for(String cat : app.keySet()){
			graph.put(cat, app.get(cat));
		}

		// rimuovo le categorie con gli spazi
		for(String cat : toRemove){
			graph.remove(cat);
		}


		ObjectMapper writerCrawlerResult = new ObjectMapper();
		writerCrawlerResult.writerWithDefaultPrettyPrinter().writeValue(new File("GraphWikipedia"), graph);
		System.out.println("Final graph size: "+graph.size());

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
		JsonObject jOb = new JsonObject();
		try{
			Scanner in = new Scanner(new InputStreamReader(con.getInputStream()));  
			JsonParser parser = new JsonParser(); 
			jOb = parser.parse(in.nextLine()).getAsJsonObject();
		}
		catch(Exception e){
			System.out.println("Connection timed out: recall method ");
			jOb = getJsonResponse(targetURL);
		}
		return jOb;
	}


	/**
	 * read Category by Taxonomy CSV. Input file contains all categories used from Taxonomy
	 * @param csvFile
	 * @param labeled
	 * @return 
	 * @return
	 * @throws IOException
	 */
	public static  Set<String> returnCategoriesFromTaxonomyCSV(String csvFile) throws IOException{

		String line = "";
		String cvsSplitBy = ",";
		Map<String, List<String>> dataMap = new HashMap<String, List<String>>();

		BufferedReader br = new BufferedReader(new FileReader(csvFile));

		while ((line = br.readLine()) != null) {
			// use comma as separator
			String[] csvData = line.split(cvsSplitBy); 
			List<String> data = new ArrayList<String>();
			if(csvData.length>=2){
				for(int i =0;i<csvData.length-1;i++){
					data.add(csvData[i].trim());
				}
				String key = csvData[csvData.length-1].trim().replace("en.wikipedia.org/wiki/Category:", "");
				if(!key.equals(""))
					dataMap.put(key, data);
			}
		}
		return dataMap.keySet();
	}  




	/**
	 * return graph in adjacency_list structure
	 * @param filePath
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static  HashMap<String, AdjacencyListRow> returnAdjacencyListFromFile(String filePath) throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		CrawlerResult crawlerResult = null;
		try {
			crawlerResult = mapper.readValue(new File(filePath), new TypeReference<CrawlerResult>() {});
		} catch (Exception e) {
			return mapper.readValue(new File(filePath), new TypeReference<HashMap<String, AdjacencyListRow>>() {});

		}

		return crawlerResult.getAdjacencyList();
	}



	/**
	 * Used to do inverse of cosine similarity with 2 vector
	 * @param vectorA
	 * @param vectorB
	 * @return
	 */
	public static double cosineSimilarityInverse(float[] vectorA, float[] vectorB) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		if(vectorA!=null && vectorB!=null && vectorA.length==vectorB.length){
			for (int i = 0; i < vectorA.length; i++) {
				dotProduct += vectorA[i] * vectorB[i];
				normA += vectorA[i] * vectorA[i];
				normB += vectorB[i] * vectorB[i];
			}   
		}

		if(dotProduct == 0 || (normA * normB) == 0)
			return 0;
		else
			return Math.asin((dotProduct) / (Math.sqrt(normA * normB)));
	}


	/**
	 * 
	 * @param graph
	 * @param categoriesToMark
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public static HashMap<String, AdjacencyListRow> markGraph(HashMap<String, AdjacencyListRow> graph, Set<String> categoriesToMark) throws JsonGenerationException, JsonMappingException, IOException{

		int countMarked = 0;
		for(String category : categoriesToMark){
			if(graph.containsKey(category)){
				graph.get(category).setTaxonomyCategory(true);
				countMarked++;
			}
			else if (graph.containsKey(category.replace("_", " "))){
				graph.get(category.replace("_", " ")).setTaxonomyCategory(true);
				countMarked++;
			}
		}

		System.out.println("Marked nodes: "+countMarked);
		ObjectMapper writerCrawlerResult = new ObjectMapper();
		writerCrawlerResult.writerWithDefaultPrettyPrinter().writeValue(new File("signedGraphWikipedia"), graph);

		return graph;

	}






}

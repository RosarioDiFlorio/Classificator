package datasetCreatorFromTaxonomy.innen;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class TaxonomyRequester
{
	private static final Logger logger = LoggerFactory.getLogger(TaxonomyRequester.class);

	private String taxonomyUri;


	public TaxonomyRequester(String uri){
		this.taxonomyUri = uri;
	} 

	/**
	 * Returns the list of concepts defined in the specified taxonomy
	 * @throws IOException 
	 */
	public JsonArray returnConceptsList () throws IOException{
		String targetURL="http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts?scheme-uri="+this.taxonomyUri;
		return sendRequestForJsonArrayResponse(targetURL);
	}

	/**
	 * Returns the list of top concepts defined in the specified taxonomy
	 * @return
	 * @throws IOException
	 */
	public JsonArray returnTopConceptsList () throws IOException{
		String targetURL="http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts/top?scheme-uri="+this.taxonomyUri;
		return sendRequestForJsonArrayResponse(targetURL);
	}


	/**
	 * Returns the list of leaves (concepts with no children) defined in the specified taxonomy
	 * @return
	 * @throws IOException
	 */
	public JsonArray returnLeafList () throws IOException{
		String targetURL="http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts/leaf?scheme-uri="+this.taxonomyUri;
		return sendRequestForJsonArrayResponse(targetURL);
	}

	/**
	 * Returns the concept with the specified URI, defined in the selected taxonomy
	 * @param targetURL
	 * @return
	 * @throws IOException
	 */
	public JsonObject returnConceptByURI(String conceptURI) throws IOException{
		String targetURL = "http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts/concept?scheme-uri="+this.taxonomyUri+"&concept-uri="+conceptURI;
		return sendRequestForJsonObjectResponse(targetURL);
	}


	/**
	 * Returns the list of concepts defined in the specified taxonomy and belonging to the specified level of depth (ONLY for hierarchical taxonomies)
	 * @throws IOException 
	 */
	public JsonArray returnConceptsFromLevel(int level) throws IOException{
		String targetURL = "http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts/level/"+level+"?scheme-uri="+taxonomyUri;
		return sendRequestForJsonArrayResponse(targetURL);
	}




	/**
	 * Returns the list of concepts defined in the specified taxonomy and corresponding to the search criteria set
	 * @param parentUri
	 * @param ancestorUri
	 * @param childUri
	 * @param descendentUri
	 * @param level
	 * @param condition
	 * @return JsonArray concepts
	 * @throws IOException 
	 */
	public JsonArray returnConceptsSearch(String parentUri,String ancestorUri, String childUri,String descendentUri, int level, String condition) throws IOException{
		String targetURL = "http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts/search?scheme-uri="+taxonomyUri;
		if(parentUri!=null)
			targetURL+="&parent-uri="+parentUri;
		if(ancestorUri!=null)
			targetURL+="&ancestor-uri="+ancestorUri;
		if(childUri!=null)
			targetURL+="&child-uri="+childUri;
		if(descendentUri!=null)
			targetURL+="&descendent-uri="+descendentUri;
		if(level>=0)
			targetURL+="&level="+level;
		if(condition!=null)
			targetURL+="condition="+condition;
		logger.debug(targetURL);
		return sendRequestForJsonArrayResponse(targetURL);

	}




	/**
	 * send a "GET" request
	 * @param targetURL to send request
	 * @return JsonObject
	 * @throws IOException
	 */
	public JsonObject sendRequestForJsonObjectResponse(String targetURL) throws IOException{
		URL url = new URL(targetURL);
		//System.out.println(url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("GET");
		Scanner in = new Scanner(
				new InputStreamReader(con.getInputStream()));
		JsonParser parser = new JsonParser(); 
		JsonObject jOb = parser.parse(in.nextLine()).getAsJsonObject();
		in.close();
		return jOb;

	}


	/**
	 * send a "GET" request
	 * @param targetURL to send request
	 * @return JsonArray
	 * @throws IOException
	 */
	public JsonArray sendRequestForJsonArrayResponse(String targetURL) throws IOException{
		URL url = new URL(targetURL);
		//System.out.println(url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("GET");
		Scanner in = new Scanner(
				new InputStreamReader(con.getInputStream()));
		JsonParser parser = new JsonParser(); 
		JsonArray jOb = parser.parse(in.nextLine()).getAsJsonArray();
		in.close();
		return jOb;

	}


	public String getTaxonomyUri() {
		return taxonomyUri;
	}

	public void setTaxonomyUri(String taxonomyUri) {
		this.taxonomyUri = taxonomyUri;
	}


	public static void main(String[] args) throws IOException{
		TaxonomyRequester requester = new TaxonomyRequester("http://www.wheesbee.eu/taxonomy");

		JsonArray response = requester.returnConceptsSearch(null, null, null, null, 1, null);
		System.out.println(response);
	}



}

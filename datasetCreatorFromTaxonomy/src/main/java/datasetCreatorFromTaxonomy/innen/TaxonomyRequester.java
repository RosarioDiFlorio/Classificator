package datasetCreatorFromTaxonomy.innen;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Hello world!
 *
 */
public class TaxonomyRequester
{

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
		return sendRequest(targetURL);
	}
	
	public JsonArray returnLeafList () throws IOException{
		String targetURL="http://taxonomies.innovationengineering.eu/services/rest/taxonomy/concepts/leaf?scheme-uri="+this.taxonomyUri;
	    return sendRequest(targetURL);
	}
	
	
	public JsonArray sendRequest(String targetURL) throws IOException{
		URL url = new URL(targetURL);
		//System.out.println(url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("GET");
		InputStream response = con.getInputStream();
		Scanner in = new Scanner(
		        new InputStreamReader(con.getInputStream()));
		JsonParser parser = new JsonParser(); 
	    JsonArray jOb = parser.parse(in.nextLine()).getAsJsonArray();
	    return jOb;
		
	}
	
	
	public static void main(String[] args) throws IOException{
		TaxonomyRequester requester = new TaxonomyRequester("http://www.wheesbee.eu/taxonomy");
		
		JsonArray response = requester.returnConceptsList();
		System.out.println(response);
	}



}

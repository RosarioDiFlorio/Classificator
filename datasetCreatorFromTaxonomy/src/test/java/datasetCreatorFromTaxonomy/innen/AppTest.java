package datasetCreatorFromTaxonomy.innen;

import java.io.IOException;

import com.google.gson.JsonArray;

/**
 * Unit test for simple App.
 */
public class AppTest 
{

	public static void main(String[] args) throws IOException{
		TaxonomyRequester requester = new TaxonomyRequester("http://www.wheesbee.eu/taxonomy");	
		//JsonObject response = requester.returnConceptByURI("http://www.wheesbee.eu/taxonomy#100147");
		JsonArray response = requester.returnConceptsSearch(null, null, "http://www.wheesbee.eu/taxonomy#100147", null, -1, null);
		System.out.println(response);		
	}
	
}

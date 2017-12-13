package datasetCreatorFromTaxonomy.innen;

import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;


/**
 * Unit test for simple App.
 */
public class AppTest 
{

	public static void main(String[] args) throws IOException{

		TaxonomyRequester requester = new TaxonomyRequester("http://www.wheesbee.eu/taxonomy");

		
			JsonArray topConcept = requester.returnTopConceptsList();
			for(JsonElement element : topConcept){
				String uri = element.getAsJsonObject().get("uri").getAsString();
				System.out.println(element.getAsJsonObject().get("prefLabel").getAsJsonObject().get("multiLanguageLabelsMap").getAsJsonObject().get("ENGLISH").getAsString());
				JsonArray childs = requester.returnConceptsSearch(uri, null, null, null, -1, "law");
				for(JsonElement child : childs){
					System.out.println("------->"+child.getAsJsonObject().get("prefLabel").getAsJsonObject().get("multiLanguageLabelsMap").getAsJsonObject().get("ENGLISH").getAsString()+"  uri:  "+child.getAsJsonObject().get("uri").getAsString());
				}

		}

	}

	public void requestByUri(TaxonomyRequester requester) throws IOException{
		JsonArray response = requester.returnConceptsSearch(null, null, "http://www.wheesbee.eu/taxonomy#100147", null, 2, null);
		for(JsonElement element : response){
			String uri = element.getAsJsonObject().get("uri").getAsString();
			System.out.println(uri);
			System.out.println(requester.returnConceptByURI(uri));
		}
	}

}

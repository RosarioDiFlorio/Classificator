package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.FileManager;




public class RenovatorTaxonomy 
{

	public static void main(String [] args) throws IOException{
		final String  inputFileName = "wheesbee_taxonomy_general_1.0.skos.rdf";
		Model model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open(inputFileName);
		if (in == null) {
			throw new IllegalArgumentException ( "File: " + inputFileName + " not found");
		}
		model.read(new InputStreamReader(in), "");
		in.close();


		System.out.println("Starting change dependency... ");
		//PER CAMBIARE I BROADER ED I NARROWER
		model = changeProperty(model);
		System.out.println("End change dependency... ");

		System.out.println("Starting delete concept... ");
		//PER ELIMINARE I CONCEPT CHE NON SERVONO
		model = deleteConcept(model);
		System.out.println("End delete concept... ");

		System.out.println("Starting add wikipedia link... ");
		//AGGIUNGERE I LINK WIKIPEDIA
		model = addWikipediaReferences(model);
		System.out.println("End add wikipedia link... ");

		//DUMP DELLA TASSONOMIA
		FileWriter writer = new FileWriter("newWheesbee.rdf");
		model.write(writer);



	}


	public static Model deleteConcept(Model model) throws IOException{
		BufferedReader br2 = new BufferedReader(new FileReader("categoriesToRemove.txt"));
		String readed2 = br2.readLine();
		Set<Resource> resourceList = new HashSet<Resource>();
		while(readed2!=null){
			resourceList.add(model.getResource(readed2));
			model = delete(model,resourceList);
			readed2 = br2.readLine();
		}

		return model;
	}


	/**
	 * this methods add wikipedia link to concept, The links are read from file wikipediaReferences.txt
	 * @param model
	 * @return model
	 * @throws IOException
	 */
	private static Model addWikipediaReferences(Model model) throws IOException {
		StmtIterator statementList = null;
		BufferedReader br = new BufferedReader(new FileReader("wikipediaReferences.txt"));
		String line = br.readLine();
		List<Statement> statementToAdd = new ArrayList<Statement>();
		while(line!=null){
			String [] splitted = line.split(",");
			String category = splitted[splitted.length-2];
			String []wikipediaCategory = splitted[splitted.length-1].split(":");
			statementList = model.listStatements();

			Statement statement;
			while(statementList.hasNext()){

				statement = statementList.next();
				// Per i concetti creiamo gli statement exactMatch
				if(statement.getPredicate().getLocalName().equals("prefLabel") && statement.getObject().toString().contains("@en") && (statement.getObject().toString().replace("@en", "").equals(category))){
					if(statement.getObject().toString().replace("@en", "").equals(category)){
						//ResourceImpl object = new ResourceImpl("https://en.wikipedia.org/wiki/Category:"+wikipediaCategory[1]);
						System.out.println("https://"+splitted[splitted.length-1]);
						ResourceImpl object = new ResourceImpl("https://"+splitted[splitted.length-1]);
						PropertyImpl property = null;
						if(statement.getObject().toString().replace("@en", "").equals(wikipediaCategory[1].replaceAll("_", " ").toLowerCase())){
							property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#exactMatch");
						}
						else{
							property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#majorMatch");
						}
						Statement newStatement = new StatementImpl(statement.getSubject(), property, object);
						System.out.println(newStatement);
						statementToAdd.add(newStatement);

					}
				}
			}

			model.add(statementToAdd);


			line = br.readLine();
		}

		return model;
	}




	/**
	 * method to delete concept tree from root. Is a recursive method, called the firt time by deleteConcecp method
	 * @param model
	 * @param resourceList
	 * @return
	 */
	public static Model delete(Model model, Set<Resource> resourceList){

		Set<Resource> narrowersNode = new HashSet<Resource>();
		StmtIterator statementList1 = model.listStatements();

		Statement statement;

		//FIRST PHASE, SEARCH NARROWER RESOURCES OF CURRENT RESOURCES TO DELETE
		while(statementList1.hasNext()){
			statement = statementList1.next();
			for(Resource resource : resourceList){
				if(statement.getSubject().getNameSpace().equals(resource.getNameSpace()) && statement.getPredicate().getURI().contains("narrower")){
					narrowersNode.add((Resource) statement.getObject());
				}
			}

		}

		//IF NARROWER SET IS NOT EMPY, MAKE RECURSIVE TASK
		if(narrowersNode.size()>0)
			model = delete(model,narrowersNode);


		// SECOND PHASE, TO DELETE CURRENT STATEMENT ABOUT CURRENT RESOURCE
		List<Statement> statementToRemove = new ArrayList<Statement>();
		StmtIterator statementList2 = model.listStatements();
		while(statementList2.hasNext()){
			statement = statementList2.next();
			for(Resource resource : resourceList){
				if(statement.getSubject().getNameSpace().equals(resource.getNameSpace()) || statement.getObject().toString().equals(resource.getNameSpace())){
					statementToRemove.add(statement);
				}
			}
		}

		model.remove(statementToRemove);

		return model;
	}


	/**
	 * this method change narrower and broader property into concept, readed from categoriesToChange.txt file In this file, the pairs are: narrower,newBroader
	 * @param model
	 * @param statementList
	 * @return
	 * @throws IOException
	 */
	public static Model changeProperty(Model model) throws IOException{
		StmtIterator statementList = model.listStatements();
		Statement statement = null;
		BufferedReader br = new BufferedReader(new FileReader("categoriesToModify.txt"));
		String readed = br.readLine();
		List<Statement> statementToRemove = new ArrayList<Statement>();
		List<Statement> statementToAdd = new ArrayList<Statement>();
		List<String> idToChange = new ArrayList<String>();

		while(readed!=null){
			boolean added = false;
			String []components= readed.split(",");
			idToChange.add(components[0]);
			while(statementList.hasNext()){
				statement = statementList.next();
				if(statement.getSubject().getNameSpace().equals(components[0]) && statement.getPredicate().getLocalName().equals("broader")){
					added=true;					
					//Creo il nuovo statement
					ResourceImpl object = new ResourceImpl(components[1]);
					Statement newStatement = new StatementImpl(statement.getSubject(), statement.getPredicate(), object);
					statementToRemove.add(statement);
					statementToAdd.add(newStatement);

					//creo lo statement inverso, per il link da padre a figlio
					PropertyImpl property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#narrower");
					ResourceImpl subject = new ResourceImpl(components[1]);
					Statement newStatement2 = new StatementImpl(subject,  property, statement.getSubject());
					statementToAdd.add(newStatement2);

				}
			}
			if(!added){ // allora significa che era un topConcept che diventa figlio
				ResourceImpl subject = new ResourceImpl(components[0]);
				PropertyImpl propertyNarrower = new PropertyImpl("http://www.w3.org/2004/02/skos/core#narrower");
				ResourceImpl object = new ResourceImpl(components[1]);
				PropertyImpl propertyBroader = new PropertyImpl("http://www.w3.org/2004/02/skos/core#broader");
				Statement statementBroaderToNarrower = new StatementImpl(subject,propertyBroader,object);
				Statement statementNarrowerToBroader = new StatementImpl(object,propertyNarrower,subject);

				statementToAdd.add(statementNarrowerToBroader);
				statementToAdd.add(statementBroaderToNarrower);

			}
			model.remove(statementToRemove);
			statementToRemove = new ArrayList<Statement>();
			statementList = model.listStatements();
			readed = br.readLine();
		}

		//RIMUOVO TUTTI I VECCHI NARROWER DEI VECCHI GENITORI , PER ROMPERE LA DIPENDENZA DEI CONCETTI CON I VECCHI BROADER
		statementToRemove = new ArrayList<Statement>();
		while(statementList.hasNext()){
			statement = statementList.next();
			for(String id : idToChange){

				if(statement.getObject().toString().contains(id) && statement.getPredicate().getLocalName().equals("narrower")){
					statementToRemove.add(statement);
				}
			}

		}



		model.remove(statementToRemove);
		model.add(statementToAdd);

		return model;
	}



	public static Model addExactMatch(Model model, StmtIterator statementList, String id,String extactMatch){
		while(statementList.hasNext()){
			Statement stm = statementList.next();
			if(stm.getSubject().getLocalName().equals(id)){
				Resource exactMatchRes = new ResourceImpl(extactMatch);
				Property exactMatchProp = new PropertyImpl("http://www.w3.org/2004/02/skos/core#exactMatch");
				Statement exactMatchStm = new StatementImpl(stm.getSubject(), exactMatchProp, exactMatchRes);
				model.add(exactMatchStm);
			}
		}
		return model;      
	}




}


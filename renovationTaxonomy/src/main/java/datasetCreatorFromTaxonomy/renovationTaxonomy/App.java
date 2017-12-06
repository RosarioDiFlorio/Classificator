package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.FileManager;




public class App 
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
		StmtIterator statementList = model.listStatements();
		
		
		
		
		//PER CAMBIARE I BROADER ED I NARROWER
		model = changeProperty(model,statementList);


		//PER ELIMINARE I CONCEPT CHE NON SERVONO
		BufferedReader br2 = new BufferedReader(new FileReader("categoriesToRemove.txt"));
		String readed2 = br2.readLine();
		Set<Resource> resourceList = new HashSet<Resource>();
		while(readed2!=null){
			System.out.println(readed2);
			resourceList.add(model.getResource(readed2));
			model = delete(model,resourceList);
			readed2 = br2.readLine();
		}
		FileWriter writer = new FileWriter("newWheesbee.rdf");
		model.write(writer);

	}


	/**
	 * method to delete concept tree from root 
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
					System.out.println(statement.toString());
				}
			}
		}

		model.remove(statementToRemove);

		return model;
	}
	
	
	public static Model changeProperty(Model model, StmtIterator statementList) throws IOException{
		Statement statement = null;
		BufferedReader br = new BufferedReader(new FileReader("categoriesToModify.txt"));
		String readed = br.readLine();
		List<Statement> statementToRemove = new ArrayList<Statement>();
		List<Statement> statementToAdd = new ArrayList<Statement>();
		List<String> idToChange = new ArrayList<String>();
		
		while(readed!=null){
			boolean added = false;
			String []components= readed.split(",");
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
		while(statementList.hasNext()){
			
			
		}
		
		
		
		
		
		model.add(statementToAdd);
		
		return model;
	}




}


package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
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

		Statement statement = statementList.next();
		Set<Resource> resourceList = new HashSet<Resource>();
		resourceList.add(model.getResource("http://www.wheesbee.eu/taxonomy#100145"));

		model = delete(model,resourceList);
		
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




}


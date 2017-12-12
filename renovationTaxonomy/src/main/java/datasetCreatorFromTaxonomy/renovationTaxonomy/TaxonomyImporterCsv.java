package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.FileManager;

import eu.innovationengineering.taxonomy.commons.utils.TranslateUtils;

public class TaxonomyImporterCsv {

  public static void main(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException{

    int count= 0;
    Map<String,ConceptBean> mapOfConcepts = new HashMap<>();


    String csvFile ="materials.csv";
    String line = "";
    String cvsSplitBy = ",";
    String fileToSave ="newMaterials.rdf";
    String fileEmptyModel ="materials.rdf";
    int limit = 2;  



    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] csvData = line.split(cvsSplitBy);

        if (csvData.length > limit){
          String name= csvData[csvData.length-1].trim().replace(" ", "_");
          if(mapOfConcepts.containsKey(name)){
            mapOfConcepts.get(name).addBroaders(mapOfConcepts.get(csvData[csvData.length-2].trim().replace(" ", "_")).getId());
            mapOfConcepts.get(csvData[csvData.length-2].trim().replace(" ", "_")).addNarrower(mapOfConcepts.get(name).getId());
          }else{         
            ConceptBean concept = new ConceptBean();
            concept.setId(++count+"");
            concept.setName(name);          
            concept.addBroaders(mapOfConcepts.get(csvData[csvData.length-2].trim().replace(" ", "_")).getId());        
            mapOfConcepts.get(csvData[csvData.length-2].trim().replace(" ", "_")).addNarrower(concept.getId());
            mapOfConcepts.put(concept.getName(), concept);
          }
        }else{//create top concept
          ConceptBean topConcept = new ConceptBean();
          String name = csvData[limit-1].trim().replace(" ", "_");
          topConcept.setId((++count)+"");
          topConcept.setName(name);
          topConcept.setTopConcept(true);
          mapOfConcepts.put(name, topConcept);
        }
      }
      System.out.println(mapOfConcepts.size());

      Model model = createEmptyModel(fileEmptyModel);
      for(String nameConcept:mapOfConcepts.keySet()){
        ConceptBean concept = mapOfConcepts.get(nameConcept);
        if(concept.getBroaders().isEmpty()){
          Resource subject = new ResourceImpl(model.getNsPrefixURI("").replace("#", ""));
          Property property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#hasTopConcept");
          Resource object = new ResourceImpl(model.getNsPrefixURI("")+concept.getId());
          Statement stmTopConcept = new StatementImpl(subject, property, object);
          model.add(stmTopConcept);
        }
        model = createConcept(model,concept);
      }

      FileWriter writer = new FileWriter(new File(fileToSave));
      model.write(writer);

    }
  }




  public static Model createConcept(Model model, ConceptBean concept) throws MalformedURLException, ParserConfigurationException, SAXException, IOException {
    List<Statement> toAdd = new ArrayList<>();
    Resource subject = new ResourceImpl(model.getNsPrefixURI("")+concept.getId());


    //create skos:inScheme
    Property property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#inScheme");
    Resource object = new ResourceImpl(model.getNsPrefixURI("").replace("#", "")); 
    toAdd.add(new StatementImpl(subject, property, object));

    //create toConceptOf 
    if(concept.isTopConcept()){
      property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#topConceptOf");
      object = new ResourceImpl(model.getNsPrefixURI("").replace("#", ""));      
      toAdd.add(new StatementImpl(subject, property, object));
    }


    //create narrowers
    for(String idNarrower:concept.getNarrowers()){
      property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#narrower");
      object = new ResourceImpl(model.getNsPrefixURI("")+idNarrower);    
      toAdd.add(new StatementImpl(subject, property, object));
    }

    //create broader
    if(!concept.getBroaders().isEmpty()){
      for(String broader: concept.getBroaders()){
        property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#broader");
        object = new ResourceImpl(model.getNsPrefixURI("")+broader);
        toAdd.add(new StatementImpl(subject, property, object));
      }
    }

    //create prefLabel
    property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#prefLabel");
    //en
    Literal l = model.createLiteral(concept.getName(), "en");
    toAdd.add( new StatementImpl(subject, property, l));
    //it
    l = model.createLiteral(TranslateUtils.translate(concept.getName().replace("_", " "), "it").replace(" ", "_"), "it");
    toAdd.add( new StatementImpl(subject, property, l));


    //create Concept
    property = new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    object = new ResourceImpl("http://www.w3.org/2004/02/skos/core#Concept");
    toAdd.add( new StatementImpl(subject, property, object));

    model.add(toAdd);
    return model;
  }




  public static Model createEmptyModel(String inputFileName) throws IOException{
    Model model = ModelFactory.createDefaultModel();
    InputStream in = FileManager.get().open(inputFileName);
    if (in == null) {
      throw new IllegalArgumentException ( "File: " + inputFileName + " not found");
    }
    model.read(new InputStreamReader(in), "");
    in.close();
    return model;
  }


}

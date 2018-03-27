package datasetCreatorFromTaxonomy.renovationTaxonomy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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

public class TaxonomyImporterCSV {
  private static int idCount = 0;
  private static Map<String,ConceptBean> conceptsMap;
  private String taxonomyCsvFile;

  public TaxonomyImporterCSV(String taxonomyCsvFile){
    conceptsMap = new HashMap<>();
    this.taxonomyCsvFile = taxonomyCsvFile;
  }

  public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException{
    TaxonomyImporterCSV importer = new TaxonomyImporterCSV("wheesbee2.csv");
    Set<ConceptBean> concepts = importer.getConcepts();
    Model model = loadEmptyModel("wheesbee2.rdf");
    model = importer.updateModel(model, concepts);
    FileWriter writer = new FileWriter(new File("newWheesbee2.rdf"));
    model.write(writer);
  }
  
  
  

  public static Model loadEmptyModel(String inputFileName) throws IOException{
    Model model = ModelFactory.createDefaultModel();
    InputStream in = FileManager.get().open(inputFileName);
    if (in == null) {
      throw new IllegalArgumentException ( "File: " + inputFileName + " not found");
    }
    model.read(new InputStreamReader(in), "");
    in.close();
    return model;
  }

  public  Model updateModel(Model model, Set<ConceptBean> concepts) throws MalformedURLException, ParserConfigurationException, SAXException, IOException{
    for(ConceptBean concept:concepts){
      List<Statement> toAdd = new ArrayList<Statement>();
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

        property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#hasTopConcept");      
        ResourceImpl tmpSub = new ResourceImpl(model.getNsPrefixURI("").replace("#", ""));
        toAdd.add(new StatementImpl(tmpSub, property, subject));
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

      //create exactMatch
      if(concept.getMatches().hasExacts()){
        for(String exact: concept.getMatches().getExacts()){
          object = new ResourceImpl("https://"+exact);
          property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#exactMatch");
          toAdd.add(new StatementImpl(subject, property, object));
        }
      }


      //create majorMatch
      if(concept.getMatches().hasMajors()){
        for(String major: concept.getMatches().getMajors()){
          object = new ResourceImpl("https://"+major);
          property = new PropertyImpl("http://www.w3.org/2004/02/skos/core#majorMatch");
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
    }
    return model;
  }


  private ConceptBean getConcept(String name){
    ConceptBean concept = new ConceptBean();
    if(conceptsMap.containsKey(name))
      concept = conceptsMap.get(name);      
    else{
      concept.setName(name);
      concept.setId(idCount+"");
      conceptsMap.put(name, concept);
      idCount++;
    }
    return concept;
  }

  public Set<ConceptBean> getConcepts() throws IOException{
    Set<ConceptBean> concepts = new HashSet<>();

    try (
        Reader reader = Files.newBufferedReader(Paths.get(this.taxonomyCsvFile));
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        ){
      List<CSVRecord> csvRecords = csvParser.getRecords();

      for (CSVRecord record : csvRecords) {

        for(int i = 0;i<record.size()-1;i++){
          String nameConcept = record.get(i);
          ConceptBean concept = getConcept(nameConcept);
          try{
            String broaderName = record.get(i-1);
            concept.addBroaders(getConcept(broaderName).getId());
          }catch (Exception e) {
            concept.setTopConcept(true);
          }

          String narrowerName = record.get(i+1);
          if(narrowerName.contains("en.wikipedia")){
            WikiCategoryMatch match = new WikiCategoryMatch();
            String wiki_link = narrowerName;
            //System.out.println(wiki_link+"   "+ record.size());
            if(nameConcept.equals(wiki_link.replace("en.wikipedia.org/wiki/Category:", "").toLowerCase())){
              match.addExact(wiki_link);
            }else{
              match.addMajor(wiki_link);
            }
            concept.setMatches(match);
          }else{
            concept.addNarrower(getConcept(narrowerName).getId());
          }
          conceptsMap.put(nameConcept, concept);
          concepts.add(concept);  
        }
      }
    }
    return concepts;
  }











}

package eu.innovation.engineering.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.wikipedia.DocumentInfo;
import eu.innovation.engineering.wikipedia.WikipediaMiner;

public class Test {

  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    main3(args);
  }

  public static void main4(String[] args) throws JsonGenerationException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    //    createMapDataset("D:/Development/Datasets/dataset_tassonomia");
    //fixNameDataset("data/dataset_tassonomia");
    //main3(args);
    Map<String,Set<String>> map = new HashMap<>();
    Set<String> ids = WikipediaMiner.getIdsMemberByType("Category:Materials", "subcat", 14);
    //map.put("Category:Materials",ids);

    for(String id:ids){
      Set<String> list = WikipediaMiner.getIdsMemberByType(id, "subcat", 14);

      //      Set<String> list =  WikipediaMiner.getIdsMemberByType(id, "page", 0);
      map.put(id, list);
      //map.put(id, WikipediaMiner.getIdsMemberByType(id, "page", 0));
    }
    List<String> toRemove = new ArrayList<>();
    for(String id: map.keySet()){
      for(String idSub: map.get(id)){
        if(map.keySet().contains(idSub))
          toRemove.add(idSub);
      }
    }

    toRemove.stream().forEach(s-> map.remove(s));

    List<String> names = new ArrayList<>();
    PrintWriter p= new PrintWriter(new File("materials.txt"));
    for(String id:map.keySet()){
      String name = WikipediaMiner.getPageInfoById(id).get("title").getAsString();
      names.add(name);
      p.println(name.replace("Category:", "").toLowerCase());
      for(String subid:map.get(id)){
        name = WikipediaMiner.getPageInfoById(subid).get("title").getAsString();
        p.println("\t"+name.replace("Category:", "").toLowerCase());
      }
      p.flush();

    }
    p.close();
    Collections.sort(names);
    /* for(String name: names){
      System.out.println(name.replace("Category:", "").toLowerCase());
    }*/
  }


  public static void main3(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException{
    Map<String, List<String>> csvMap = read("wheesbee_cat_recovery.csv", false);

    System.out.println(csvMap.keySet().size());
    int count = 0;

    String pathDataset = "data/dataset";
    new File(pathDataset).mkdir();
    Set<String> toExtract = new HashSet<>();
    for(String uriWiki : csvMap.keySet()){

      List<String> parents = csvMap.get(uriWiki);
      String path = pathDataset;
      for(int i =0; i<parents.size();i++){
        path = path+"/"+parents.get(i);
        new File(path).mkdir();
      }
      parents = new ArrayList<>();
      parents.add(path);
      csvMap.replace(uriWiki, parents);
      toExtract.add(uriWiki);
      count++;

      Map<String, Set<DocumentInfo>> results = WikipediaMiner.buildDataset(toExtract, 0, true, 1000);
      for(String key : results.keySet()){
        for( DocumentInfo doc: results.get(key)){
          PrintWriter writer = new PrintWriter(new File(csvMap.get(key).get(0)+"/"+doc.getId()));
          writer.println(doc.getText());
          writer.flush();
          writer.close();
        }
        System.out.println("writed in "+csvMap.get(key).get(0));
      }
      count = 0;
      toExtract = new HashSet<>();

    }




  }






  public static void main2(String[] args) throws IOException, InterruptedException, ExecutionException{

    Map<String, List<String>> mapWiki = read("categories-hierarchy.csv", true);
    ObjectMapper mapper = new ObjectMapper();

    System.out.println(mapWiki.keySet().size());

    //Map<String, String> leafMap = buildLeafMap(mapWiki.keySet());

    Map<String, String> leafMap = mapper.readValue(new File("leafMap.json"), new TypeReference<Map<String,String>>() {});

    Map<String,Set<String>> rootCats = new HashMap<>();
    for(String leaf:mapWiki.keySet()){
      if(!mapWiki.get(leaf).isEmpty()){
        if(rootCats.get(mapWiki.get(leaf).get(0))!=null){
          Set<String> tmp = rootCats.get(mapWiki.get(leaf).get(0));
          tmp.add(leaf);
          rootCats.replace(mapWiki.get(leaf).get(0), tmp);
        }else{
          Set<String> tmp = new HashSet<>();
          tmp.add(leaf);
          rootCats.put(mapWiki.get(leaf).get(0), tmp);
        }
      }

    }


    System.out.println(rootCats.keySet());

    String pathDataset ="data/alchemyTax"; 
    WikipediaMiner.buildStructureFolder(rootCats.keySet(), pathDataset);
    Map<String, Set<DocumentInfo>> datasetMap = WikipediaMiner.buildDataset(leafMap.keySet(), 0, true,500);
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("dataset.json"), datasetMap);
    Set<String> alreadyWritten = new HashSet<>();

    for(String c:rootCats.keySet()){
      pathDataset = pathDataset+"/"+c;
      WikipediaMiner.buildStructureFolder(rootCats.get(c), pathDataset);

      for(String wikiCat: datasetMap.keySet()){
        wikiCat = URLDecoder.decode(wikiCat,"utf-8");
        System.out.println("Wikipedia Category -> "+wikiCat+" documents -> "+datasetMap.get(wikiCat).size());
        for(DocumentInfo doc: datasetMap.get(wikiCat)){
          if(!alreadyWritten.contains(doc.getId())){
            alreadyWritten.add(doc.getId());
            PrintWriter p = new PrintWriter(new File(pathDataset+"/"+leafMap.get(wikiCat)+"/"+doc.getId()));
            p.println(doc.getTitle()+"\n"+doc.getText());
            p.flush();
            p.close();
          }             
        }
      }  
    }


  }

  public static void renameFile(String path) throws IOException {

    File root = new File(path);
    File[] list = root.listFiles();

    if (list == null)
      return;

    for (File f : list) {
      if (f.isDirectory()) {
        File from = new File(f,f.getName());
        File to = new File(f,f.getName().replace(" ", "_"));
        from.renameTo(to);
        renameFile(path+"/"+from.getName());
      } else {
        //System.out.println("File:" + f.getAbsoluteFile());
      }
    }
  }


  public static void fixNameDataset(String path){
    File root = new File(path);
    File[] childs = root.listFiles();
    for(File child: childs){
      if(child.isDirectory()){
        try {
          File newfile =new File("newfile.txt");
          if(child.renameTo(newfile)){
            System.out.println("Rename succesful");
          }else{
            System.out.println("Rename failed");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        fixNameDataset(path+"/"+child.getName());
      }

    }
    File newRoot = new File(path+"/"+root,root.getName().replace("_", " "));
    root.renameTo(newRoot);
  }

  public static void createMapDataset(String path) throws JsonGenerationException, JsonMappingException, IOException{
    Map<String,List<String>> toWrite = new HashMap<>();
    List<String> rootChild = getChilds(path);
    toWrite.put("root", rootChild);

    for(String child:rootChild){
      toWrite.putAll(createMapDatasetTask(path+"/"+child));
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("map_tax.json"), toWrite);
  }

  public static Map<String,List<String>> createMapDatasetTask(String path) throws JsonGenerationException, JsonMappingException, IOException{
    Map<String,List<String>> toWrite = new HashMap<>();
    List<String> rootChild = getChilds(path);
    if(!rootChild.isEmpty())
      toWrite.put(new File(path).getName(), rootChild);
    for(String child:rootChild){
      List<String> newphewList = getChilds(path+"/"+child);
      if(!newphewList.isEmpty()){
        toWrite.put(child, newphewList);
        for(String newPhew : newphewList){
          toWrite.putAll(createMapDatasetTask(path+"/"+child));
        }
      }
    }
    return toWrite;
  }

  public static List<String> getChilds (String path){
    File dir = new File(path);
    List<String> toReturn = new ArrayList<>();
    if(dir.isDirectory()){
      File[] subDirs = dir.listFiles();
      for(File el: subDirs){
        if(el.isDirectory())
          toReturn.add(el.getName());
      }
    }
    Collections.sort(toReturn);
    return toReturn;
  }



  public static Map<String,String> buildLeafMap(Set<String> categories) throws JsonGenerationException, JsonMappingException, IOException{
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> leafMap = new HashMap();
    for(String leaf : categories){
      String queryKey = leaf;
      List<String> pages = WikipediaMiner.searchWiki(queryKey.trim());
      LevenshteinDistance lDis = new LevenshteinDistance();
      //pages.sort((s1,s2)-> Double.compare(lDis.apply(s1.replace("Category:", "").toLowerCase(),queryKey), lDis.apply(s2.replace("Category:", "").toLowerCase(), queryKey)));


      if(!pages.isEmpty()){
        String category = "";
        int count = 0;
        boolean isAcceptable = false;
        while(!isAcceptable){
          category = WikipediaMiner.getCategory(pages.get(count),queryKey);
          if(category == null)
            continue;
          if(category.contains("Category:Disambiguation pages"))
            count++;
          else
            isAcceptable = true;
        }
        if(category != null){
          System.out.println(leaf+" - "+category);
          leafMap.put(category, leaf);
        }
      }
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File("leafMap.json"), leafMap);
    }
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("leafMap.json"), leafMap);
    return leafMap;
  }


  public static Map<String,List<String>> read(String csvFile,boolean labeled) {
    String line = "";
    String cvsSplitBy = ",";
    Map<String, List<String>> dataMap = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      if(labeled)
        line = br.readLine();
      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] csvData = line.split(cvsSplitBy); 
        List<String> data = new ArrayList<>();
        if(csvData.length>=2){
          for(int i =0;i<csvData.length-1;i++){
            data.add(csvData[i].trim());
          }
          String key = csvData[csvData.length-1].trim().replace("en.wikipedia.org/wiki/", "");
          if(dataMap.containsKey(key))
            System.out.println(key);
          if(!key.equals(""))
            dataMap.put(key, data);
        }
      }
      return dataMap;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return dataMap;
  }


}

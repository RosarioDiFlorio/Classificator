package eu.innovation.engineering.wikipedia;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveTask;

import persistence.SQLiteWikipediaGraph;

/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class DatasetTask extends RecursiveTask<Map<String,Set<DocumentInfo>>> implements Callable<Map<String,Set<DocumentInfo>>> {

  private String category;
  private int maxLevel;
  private boolean recursive;
  private int limitDocs;
  private SQLiteWikipediaGraph graph;

  /**Constructior for the Offline Versione(database required).
   * @param category
   * @param graph
   * @param limitDocs
   */
  public DatasetTask(String category,SQLiteWikipediaGraph graph,int limitDocs){
    this.category = category;
    this.graph = graph;
    this.limitDocs = limitDocs;
  }
  /**Constructor for the Online version without the database.
   * @param category
   * @param maxLevel
   * @param recursive
   * @param limitDocs
   */
  public DatasetTask(String category,int maxLevel,boolean recursive,int limitDocs){
    this.category = category;
    this.maxLevel = maxLevel;
    this.recursive = recursive;
    this.limitDocs = limitDocs;
    this.graph = graph;
  }


  @Override
  public Map<String, Set<DocumentInfo>> call() throws Exception {
    return compute();
  }

  @Override
  protected Map<String, Set<DocumentInfo>> compute() {
    Map<String, Set<DocumentInfo>> toReturn = new HashMap<>();
    try {
      Map<String, DocumentInfo> contents = new HashMap<>();
      /*
       * Oline Version.
       */     
      if(graph == null)
        contents = WikipediaMiner.getContentFromCategoryPages(category, new HashSet<String>(), recursive, 0, maxLevel,limitDocs);
      /*
       * Offline Versione
       */
      else
        contents = WikipediaMiner.getContentFromCategoryPages(category, graph, limitDocs);
      
      Set<DocumentInfo> listDocument = new HashSet<>();
      for(String idDoc: contents.keySet()){
        DocumentInfo docInfo = contents.get(idDoc);
        listDocument.add(docInfo);
      }
      toReturn.put(category, listDocument);
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    //System.out.println(category+" -> done");
    return toReturn;
  }



}

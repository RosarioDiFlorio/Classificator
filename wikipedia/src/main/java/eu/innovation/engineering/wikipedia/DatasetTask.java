package eu.innovation.engineering.wikipedia;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveTask;

/**
 * @author Rosario Di Florio (RosarioUbuntu)
 *
 */
public class DatasetTask extends RecursiveTask<Map<String,Set<DocumentInfo>>> implements Callable<Map<String,Set<DocumentInfo>>> {

  private String category;
  private int maxLevel;
  private boolean recursive;
  private int limitDocs;

  /**
   * @param category
   * @param maxLevel
   * @param recursive
   */
  public DatasetTask(String category,int maxLevel,boolean recursive,int limitDocs){
    this.category = category;
    this.maxLevel = maxLevel;
    this.recursive = recursive;
    this.limitDocs = limitDocs;
  }


  @Override
  public Map<String, Set<DocumentInfo>> call() throws Exception {
    return compute();
  }

  @Override
  protected Map<String, Set<DocumentInfo>> compute() {
    Map<String, Set<DocumentInfo>> toReturn = new HashMap<>();
    try {
      Set<String> listIdDocuments = WikipediaMiner.requestIdsPagesOfCategory(category, new HashSet<String>(), recursive, 0, maxLevel,limitDocs);
      Map<String, DocumentInfo> contents = WikipediaMiner.getContentPages(listIdDocuments);

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

    System.out.println(category+" -> done");
    return toReturn;
  }



}

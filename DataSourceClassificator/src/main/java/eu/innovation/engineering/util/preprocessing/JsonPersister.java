package eu.innovation.engineering.util.preprocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.innovation.engineering.config.PathConfigurator;

public class JsonPersister {

  /**
   * 
   * Example main on how to utilize the class.
   * @param args
   */
  public static void main(String[] args){
    List<String> intList = new ArrayList<>();
    for(int i=0;i<10;i++){
      intList.add(""+i);
    }
    saveObject(PathConfigurator.applicationFileFolder+"intList.json", intList);
    intList = loadObject(PathConfigurator.applicationFileFolder+"intList.json");    
  }



  /**
   * Save an Object into the specified path.
   * @param pathWhereSave
   * @param object
   * @return
   */
  public static <E> boolean saveObject(String pathWhereSave,E object){
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathWhereSave), object);
      System.out.println(object.getClass()+" saved into -> "+pathWhereSave);
      return true;
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return false;    
  }


  /**
   * Load an object from a specified path.
   * @param pathWhereLoad
   * @return
   */
  public static <E> E loadObject(String pathWhereLoad){  
    try {
      ObjectMapper mapper = new ObjectMapper();
      E toReturn = mapper.readValue(new File(pathWhereLoad), new TypeReference<E>() {});
      return toReturn;
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}

package eu.innovation.engineering.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Configurator {
  
  public static final int minNumPaperForTraining = 1;
  public static final int minNumPaperForTest = 1;
  public static final int numPaperForTraining = 30;
  public static final int numPaperForTest = 2;
  public static final int numFeatures = 100;

  
  public final static Set<String> getCategories() throws IOException{

    Set<String> categories = new HashSet<String>();
    FileReader fr = new FileReader(PathConfigurator.applicationFileFolder+"categories.txt");
    BufferedReader bufferedReader = new BufferedReader(fr);

    String line = bufferedReader.readLine();
    while(line!=null){
      String cat[] = line.split("/");
      if(cat.length==1)
        categories.add("/"+line);
      line=bufferedReader.readLine();
    }
    return categories;
  }
  

}

package eu.innovation.engineering.prepocessing.interfaces;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface DataReader {
  
  public Set<String> getIdPaper() throws IOException;
  public Map<String,HashMap<String,String>> categoriesWithPaper() throws IOException;
}

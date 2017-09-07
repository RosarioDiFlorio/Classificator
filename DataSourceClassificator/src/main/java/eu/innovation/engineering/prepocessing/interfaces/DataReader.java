package eu.innovation.engineering.prepocessing.interfaces;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface DataReader {
  
  public Set<String> getIds() throws IOException;
  public Map<String,HashMap<String,String>> categoriesWithIds(String pathFile) throws IOException;
}

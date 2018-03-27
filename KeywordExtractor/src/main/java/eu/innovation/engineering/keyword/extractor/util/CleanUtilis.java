package eu.innovation.engineering.keyword.extractor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class CleanUtilis {
  
  private static Set<String> blackList;
  
  
  public  static  Set<String> getBlackList(String stopWordPath){
    if (blackList == null) {
      blackList = new HashSet<String>();
      File txt = new File(stopWordPath);  
      InputStreamReader is;
      String sw = null;
      try {
        is = new InputStreamReader(new FileInputStream(txt), "UTF-8");
        BufferedReader br = new BufferedReader(is);             
        while ((sw=br.readLine()) != null)  {
          blackList.add(sw.toLowerCase());   
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return blackList;
  }


  public static void setBlacklist(Set<String> blacklist) {
    CleanUtilis.blackList = blacklist;
  }
}

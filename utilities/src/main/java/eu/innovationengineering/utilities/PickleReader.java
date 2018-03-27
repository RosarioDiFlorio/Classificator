package eu.innovationengineering.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PickleReader {
  private  Pattern drivePattern;
  private Matcher driveMatcher;

  public PickleReader(){
    drivePattern = Pattern.compile("^(([a-zA-Z])+\\:)");

  }

  public <K,V> Map<K, V> loadPklFile(String pklPath) throws IOException, InterruptedException {
    Map<K, V> dict;

    File tempScript = File.createTempFile("pklExecutor", ".py");
    try {
      try (InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream("pklExporter.py");
          OutputStream os = new FileOutputStream(tempScript);) {
        IOUtils.copy(script, os);
        os.flush();
      }

      String tempScriptPath = tempScript.getCanonicalPath().replace('\\', '/');

      driveMatcher = drivePattern.matcher(tempScriptPath);
      StringBuffer sb = new StringBuffer();
      if (driveMatcher.find()) {
        driveMatcher.appendReplacement(sb, "/mnt/" + driveMatcher.group(2).toLowerCase());
      }
      driveMatcher.appendTail(sb);
      tempScriptPath = sb.toString();

      pklPath = pklPath.replace('\\', '/');
      driveMatcher = drivePattern.matcher(pklPath);
      sb = new StringBuffer();
      if (driveMatcher.find()) {
        driveMatcher.appendReplacement(sb, "/mnt/" + driveMatcher.group(2).toLowerCase());
      }
      driveMatcher.appendTail(sb);
      pklPath = sb.toString();


      ProcessBuilder pb = new ProcessBuilder("bash", "-c", "python3 " + tempScriptPath + " " + pklPath);
      pb.redirectErrorStream(true);
      Process p = pb.start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        ObjectMapper mapper = new ObjectMapper();
        dict = mapper.readValue(reader, new TypeReference<Map<K, V>>() {});
        p.destroy();
        p.waitFor();
      }
    }
    finally {
      tempScript.delete();
    }

    return dict;
  }


}

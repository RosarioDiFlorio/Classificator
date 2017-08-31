package eu.innovationengineering.Tester;



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class CSVReader {

  public static void main(String[] args) {

    String csvFile = "listPt.csv";
    String line = "";
    String cvsSplitBy = ",";

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

      while ((line = br.readLine()) != null) {

        // use comma as separator
        String[] country = line.split(cvsSplitBy);


        Lemmatizer lemmsTest = new Lemmatizer();
        List<String> lemmas =  lemmsTest.lemmatize(country[1]);
        
        System.out.println(country[1]+" -> " + lemmas);

      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}

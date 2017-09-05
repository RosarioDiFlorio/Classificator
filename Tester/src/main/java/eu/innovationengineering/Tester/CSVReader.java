package eu.innovationengineering.Tester;



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CSVReader {

  
  public static void main(String[] args){
    read("results2.csv");
  }
  
  
  public static void read(String csvFile) {

   
    String line = "";
    String cvsSplitBy = ",";

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

      while ((line = br.readLine()) != null) {

        // use comma as separator
        String[] country = line.split(cvsSplitBy); 
        
        System.out.println(country[0]+" -> ");

      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}

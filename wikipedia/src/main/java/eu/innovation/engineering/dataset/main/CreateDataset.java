package eu.innovation.engineering.dataset.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import eu.innovation.engineering.dataset.utility.DatasetUtilities;
import eu.innovation.engineering.graph.main.AnalyzerWikipediaGraph;
import eu.innovation.engineering.graph.utility.ListAllFiles;



public class CreateDataset {

  final private static int minCut = 0;
  final private static int maxCut = 10000000;

  public static void main(String[] args) throws IOException{
    ListAllFiles fileReader = new ListAllFiles();
    String basePath = "D:/Development/Datasets/datasets_tassonomia_dijstra/";
    String basePathSrc = basePath+"dataset/";
    String basePathDstTraining = basePath+"datasets_training/";
    String basePathDstTest = basePath+"datasets_test/";
       
    List<String> fileList = fileReader.listFilesAndFilesSubDirectories(basePathSrc, new ArrayList<String>());
    new File(basePathDstTraining).mkdir();
    new File(basePathDstTest).mkdir();
    
    Set<String> pathSet = DatasetUtilities.getAllPaths(basePathSrc);
    //TRAINING
    ArrayList<String> added = buildDataset(pathSet, basePathDstTraining, basePathSrc, fileList, fileReader,450, new ArrayList<String>(), "training");

    //TEST
    buildDataset(pathSet, basePathDstTest, basePathSrc, fileList, fileReader,50,added,"test");
  }

  public static ArrayList<String>  buildDataset(Set<String> pathList,String basePathDst,String basePathSrc, List<String> fileList, ListAllFiles fileReader, int numSourceToCopy, List<String> added, String datasetType) throws IOException{
    // PER OGNNI PATH CALCOLATO

    ArrayList<String> addedToReturn = new ArrayList<String>();
    FileWriter writerLabelsTraining = new FileWriter(new File("labelsItemTrainingWithOrigin.csv"));
    FileWriter writerLabelsTest = new FileWriter(new File("labelsItemTestWithOrigin.csv"));
    writerLabelsTest.write("id,origin,firstLabel,secondLabel,thirdLabel\n");

    for(String path:pathList){
      System.out.println(path);

      //creo la folder 
      new File(basePathDst+path).mkdir();
      Set<String> leafList = new HashSet<String>();

      // CALCOLO LA LISTA DI TUTTE LE CATEGORIE FOGLIA
      for(String file: fileList){
        file = file.replace("\\", "/").replace(basePathSrc, "");
        String [] splitted = file.split("/");
        if(file.contains(path.replace("root/", ""))){
          String toSave = file.replace("/"+splitted[splitted.length-1], "");
          leafList.add(toSave);
        }
      }

      // A QUESTO PUNTO AGGIUNGO I DOCUMENTI AL PATH CORRENTE USANDO LE CATEGORIE FOGLIA CALCOLATE
      int numSource = (numSourceToCopy/leafList.size());


      AnalyzerWikipediaGraph analyzerWikipedia = new AnalyzerWikipediaGraph();
      //Per ogni foglia della lista delle categorie foglia
      for(String leaf:leafList){

        String [] leafSplitted = leaf.split("/");
        String nameLeaf = leafSplitted[leafSplitted.length-1];

        int count = 0; 
        // leggo il path del file corrente
        ArrayList<String> files = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(basePathSrc+"/"+leaf, new ArrayList<String>());
        for(String file:files){

          file = file.replace("\\", "/");
          String[] splitted = file.split("/");

          File f1 = new File(file);



          int wordCount=0;

          // conto il numero di parole del documento
          try(Scanner sc = new Scanner(new FileInputStream(file))){
            while(sc.hasNext()){
              sc.next();
              wordCount++;
            }
          }
          catch(Exception e){}


          if(!added.contains(splitted[splitted.length-1])){
            if(count>=numSource)
              break;
            else{
              count++;
              File f2 = null;
              if(datasetType.equals("training")){
                addedToReturn.add(splitted[splitted.length-1]);
                f2 = new File(basePathDst+path+"/"+nameLeaf+"_"+splitted[splitted.length-1]);
                FileUtils.copyFile(f1, f2); 

                //Dataset To evaluate classifier with training set
                //File f3 = new File(basePathDst+"/datasetToEvaluate/"+splitted[splitted.length-1]); 
                //FileUtils.copyFile(f1, f3); 
                //List<String> labels =  analyzerWikipedia.getDocumentLabelsTaxonomy(splitted[splitted.length-1]);
                //saveLabelsOnCSV(nameLeaf,labels,writerLabelsTraining,splitted);
                /////////////////////////////////////////////////
              }
              else
                if(datasetType.equals("test") && (!addedToReturn.contains(splitted[splitted.length-1]))){							
                  if(wordCount >=minCut && wordCount<=maxCut){
                    addedToReturn.add(splitted[splitted.length-1]);
                    f2 = new File(basePathDst+"/"+splitted[splitted.length-1]); 
                    //Codice per cercare le categorie dal grafo wikipedia. Creare un file CSV che contiene le categorie che il grafo ha restituito
                    List<String> labels =  analyzerWikipedia.getDocumentLabelsTaxonomy(splitted[splitted.length-1],true);
                    // se il nome della foglia è uguale al nome della labels in posizione 0. Faccio questo per prendere solo i documenti per i quali il grafo wikipedia ha una buona corrispondenza con l'etichetta usata durante la generazione del dataset
                    if(labels!=null && !labels.isEmpty()){
                      //if(nameLeaf.toLowerCase().equals(labels.get(0).toLowerCase())){
                      FileUtils.copyFile(f1, f2);
                      saveLabelsOnCSV(nameLeaf,labels,writerLabelsTest,splitted);
                      //} 
                    }
                  }
                }
            }
          }
        }
      }

    }
    writerLabelsTest.flush();
    writerLabelsTest.close();

    return addedToReturn;
  }



  /**
   * Method used to save labels on csv file 
   * @param nameLeaf
   * @param labels
   * @param writer
   * @param splitted
   * @throws IOException
   */
  public static void saveLabelsOnCSV(String nameLeaf,List<String> labels,FileWriter writer, String[] splitted) throws IOException{

    try{
      if(labels.size()>=3){
        writer.write(splitted[splitted.length-1]+","+nameLeaf+","+labels.get(0)+","+labels.get(1)+","+labels.get(2)+"\n");
      }
      else
        if(labels.size()>=2){
          writer.write(splitted[splitted.length-1]+","+nameLeaf+","+labels.get(0)+","+labels.get(1)+"\n");
        }
        else
          if(labels.size()>0){
            writer.write(splitted[splitted.length-1]+","+nameLeaf+","+labels.get(0)+"\n");
          }
    }
    catch(Exception e){
      //System.out.println(splitted[splitted.length-1]);
    }
    writer.flush();

  }

















}




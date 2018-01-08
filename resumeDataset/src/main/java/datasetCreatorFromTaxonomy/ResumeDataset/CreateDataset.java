package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateDataset {


	public static void main(String[] args) throws IOException{
		ListAllFiles fileReader = new ListAllFiles();

		String directoryWindows ="D:/TextClassifier/dataset_tassonomia/";


		ArrayList<String> fileList = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(directoryWindows, new ArrayList<String>());
		Set<String> pathList = new HashSet<String>();

		String basePathSrc = "D:/TextClassifier/dataset_tassonomia/";
		String basePathDst = "D:/Development/Datasets/dataset_trainingNew/";

		new File("D:/TextClassifier/datasets_trainingNew").mkdir();
		new File("D:/TextClassifier/datasets_testNew").mkdir();
		String basePathDstTraining = "D:/TextClassifier/datasets_trainingNew/";
		String basePathDstTest = "D:/TextClassifier/datasets_testNew/";

		/*new File("D:/TextClassifier/training/training").mkdir();
		new File("D:/TextClassifier/training/test").mkdir();
		String basePathDstTraining = "D:/TextClassifier/training/trainingApp/";
		String basePathDstTest = "D:/TextClassifier/training/testApp/";*/



		//CALCOLO TUTTE LE POSSIBILI COPPIE DI CATEGORIE E LE AGGIUNGO AL SET 
		// ESEMPIO: A/B/C/D/E/File.txt
		// A/B B/C C/D D/E  
		for(String file : fileList){
			file = file.replace("\\", "/").replace(basePathSrc, "");
			String [] splitted = file.split("/");
			int length = splitted.length-2;
			if(length>0){
				int count = length;
				while(length>0){
					pathList.add(splitted[length-1]+"/"+splitted[length]);
					length--;
				}
				pathList.add("root/"+splitted[0]);
			}
			else
				if(length==0){
					pathList.add("root/"+splitted[0]);
				}

		}

		

		//TRAINING
		ArrayList<String> added = buildDataset(pathList, basePathDstTraining, basePathSrc, directoryWindows, fileList, fileReader,450, new ArrayList<String>(), "training");

		//TEST
		buildDataset(pathList, basePathDstTest, basePathSrc, directoryWindows, fileList, fileReader,50,added,"test");
	}






	public static ArrayList<String>  buildDataset(Set<String> pathList,String basePathDst,String basePathSrc, String directoryWindows, ArrayList<String> fileList, ListAllFiles fileReader, int numSourceToCopy, ArrayList<String> added, String datasetType) throws IOException{
		// PER OGNNI PATH CALCOLATO
		
		ArrayList<String> addedToReturn = new ArrayList<String>();
		int countDuplicate = 0;
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
			
			FileWriter writerLabelsTest = new FileWriter(new File("labelsItemTest.csv"));
			AnalyzerWikipediaGraph analyzerWikipedia = new AnalyzerWikipediaGraph();
			for(String leaf:leafList){

				String [] leafSplitted = leaf.split("/");
				String nameLeaf = leafSplitted[leafSplitted.length-1];

				int count = 0; 
				ArrayList<String> files = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(directoryWindows+"/"+leaf, new ArrayList<String>());
				for(String file:files){
					file = file.replace("\\", "/");
					String[] splitted = file.split("/");

					if(!added.contains(splitted[splitted.length-1])){
						addedToReturn.add(splitted[splitted.length-1]);
						if(count>=numSource)
							break;
						else{
							count++;
							File f1 = new File(file);
							File f2 = null;
							if(datasetType.equals("training")){
								f2 = new File(basePathDst+path+"/"+nameLeaf+"_"+splitted[splitted.length-1]);
							}
							else{
								f2 = new File(basePathDst+"/"+splitted[splitted.length-1]);
								// Codice per cercare le categori dal grafo wikipedia. Creare un file CSV che contiene le categorie che il grafo ha restituito
								List<String>labels =  analyzerWikipedia.getDocumentLabels(splitted[splitted.length-1],3);
								writerLabelsTest.write(splitted[splitted.length-1]+","+labels.get(0)+","+labels.get(1)+","+labels.get(2)+"\n");
								writerLabelsTest.flush();
							}
							FileUtils.copyFile(f1, f2);  
						}
					}
				}
			}
			writerLabelsTest.flush();
			writerLabelsTest.close();
		}
		
		return addedToReturn;
	}














	


}




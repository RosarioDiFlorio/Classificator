package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.plaf.FileChooserUI;

import org.apache.commons.io.FileUtils;

public class CreateDataset {


	public static void main(String[] args) throws IOException{
		ListAllFiles fileReader = new ListAllFiles();
		String directoryWindows ="D:/TextClassifier/dataset_tassonomia";


		ArrayList<String> fileList = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(directoryWindows, new ArrayList<String>());
		Set<String> pathList = new HashSet<String>();
		String basePathSrc = "D:/TextClassifier/dataset_tassonomia/";
		String basePathDst = "D:/TextClassifier/training/datasets2/";

		/*for(int i=0;i<1;i++){
			System.out.println(i);
			if(i==0)
				basePathDst = basePathDst+"/root/";
			else
				basePathDst = basePathDst.replace("/root/", "");
			pathList = new HashSet<String>();
			for(String file : fileList){

				file = file.replace("\\", "/").replace(basePathSrc, "");
				String [] splitted = file.split("/");
				if(splitted.length-2>i)
					pathList.add(splitted[i]);
			}
			//		System.out.println(pathList);
			for(String path:pathList){
				//creo la folder 

				new File(basePathDst+path).mkdir();
				Set<String> leafList = new HashSet<String>();
				for(String file: fileList){
					file = file.replace("\\", "/").replace(basePathSrc, "");
					String [] splitted = file.split("/");
					if(splitted.length-2>i){
						if(splitted[i].equals(path)){
							String toSave = file.replace("/"+splitted[splitted.length-1], "");
							leafList.add(toSave);
						}
					}

				}
				int numSource = 2000/leafList.size()+1;

				for(String leaf:leafList){
					int count = 0;
					ArrayList<String> files = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(directoryWindows+"/"+leaf, new ArrayList<String>());
					for(String file:files){

						if(count>= numSource)
							break;
						else{
							count++;
							file = file.replace("\\", "/");
							String[] splitted = file.split("/");
							File f1 = new File(file);
							File f2 = new File(basePathDst+path+"/"+splitted[splitted.length-1]);
							FileUtils.copyFile(f1, f2);

						}
					}
				}
			}

		}*/


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
		}

		// PER OGNNI PATH CALCOLATO 
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

			int numSource = 500/leafList.size();

			for(String leaf:leafList){
				String [] leafSplitted = leaf.split("/");
				String nameLeaf = leafSplitted[leafSplitted.length-1];
				int count = 0;
				ArrayList<String> files = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(directoryWindows+"/"+leaf, new ArrayList<String>());
				for(String file:files){

					if(count>= numSource)
						break;
					else{
						count++;
						file = file.replace("\\", "/");
						String[] splitted = file.split("/");
						File f1 = new File(file);
						File f2 = new File(basePathDst+path+"/"+nameLeaf+"_"+splitted[splitted.length-1]);
						FileUtils.copyFile(f1, f2);

					}
				}

			}
		}

	}

	public static void main2(String [] args) throws IOException{
		ListAllFiles fileReader = new ListAllFiles();
		String directoryWindows ="D:/TextClassifier/dataset_tassonomia";
		String newDirectory = "D:/TextClassifier/datasets";

		ArrayList<String> fileList = (ArrayList<String>) fileReader.listFilesAndFilesSubDirectories(directoryWindows, new ArrayList<String>());
		int numDocuments = 1000;
		for(String file : fileList){
			String basePathSrc = "D:/TextClassifier/dataset_tassonomia/";
			String basePathDst = "D:/TextClassifier/training2/datasets/";
			file = file.replace("\\", "/").replace(basePathSrc, "");
			String [] splitted = file.split("/");
			String name = splitted[splitted.length-1];
			if(splitted.length>2){
				int length = splitted.length-2;

				File f1 = new File(basePathSrc+file);
				new File(basePathDst+splitted[length-1]).mkdir();
				new File(basePathDst+splitted[length-1]+"/"+splitted[length]).mkdir();
				if(fileReader.listFolders(basePathDst+splitted[length-1]+"/"+splitted[length]).size()<numDocuments){
					File f3 = new File(basePathDst+splitted[length-1]+"/"+splitted[length]+"/"+name);
					FileUtils.copyFile(f1,f3);
					length--;
					while(length>0){
						new File(basePathDst+splitted[length-1]).mkdir();
						new File(basePathDst+splitted[length-1]+"/"+splitted[length]).mkdir();

						File f2 = new File(basePathDst+splitted[length-1]+"/"+splitted[length]+"/"+name);
						FileUtils.copyFile(f1,f2);
						length--;
					}
					//sono arrivato alla fine quindi aggungo a root
					new File(basePathDst+"root").mkdir();
					new File(basePathDst+"root/"+splitted[0]).mkdir();
					File f2 = new File(basePathDst+"root/"+splitted[0]+"/"+name);
					FileUtils.copyFile(f1,f2);

				}
			}
			//Caso in cui la categoria è ti tipo law/nomefile.txt ovvero c'è solo la top category
			else{
				System.out.println(file);
				File f1 = new File(basePathSrc+file);
				new File(basePathDst+"root").mkdir();
				new File(basePathDst+"root/"+splitted[0]).mkdir();
				if(fileReader.listFolders(basePathDst+"root/"+splitted[0]).size()<numDocuments){	
					File f2 = new File(basePathDst+"root/"+splitted[0]+"/"+name);
					FileUtils.copyFile(f1,f2);
				}
			}


			/*for(int i = 0; i<splitted.length-2;i++){
					File f1 = new File(basePathSrc+file);
					if(i==0){
						//Root
						new File(basePathDst+"root").mkdir();
						new File(basePathDst+"root/"+splitted[i]).mkdir();
						File f2 = new File(basePathDst+"root/"+splitted[i]+"/"+name);
						FileUtils.copyFile(f1,f2);
						//Primo Livello
						new File(basePathDst+splitted[i]).mkdir();
						new File(basePathDst+splitted[i]+"/"+splitted[i+1]).mkdir();
						File f3 = new File(basePathDst+splitted[i]+"/"+splitted[i+1]+"/"+name);
						FileUtils.copyFile(f1,f3);
					}
					else{
						//Altri livelli
						new File(basePathDst+splitted[i]).mkdir();
						new File(basePathDst+splitted[i]+"/"+splitted[i+1]).mkdir();
						File f3 = new File(basePathDst+splitted[i]+"/"+splitted[i+1]+"/"+name);
						FileUtils.copyFile(f1,f3);

					}
				}*/



		}
	}
}




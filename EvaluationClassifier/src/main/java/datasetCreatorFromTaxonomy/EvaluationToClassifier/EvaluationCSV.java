package datasetCreatorFromTaxonomy.EvaluationToClassifier;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Hello world!
 *
 */
public class EvaluationCSV 
{
	public static void main( String[] args ) throws IOException
	{

		HashMap<String, HashSet<String>> csvGiulia = readCSV("scoreGiulia.csv",",");
		HashMap<String, HashSet<String>> csvLabels = readCSV("testLabelOrigin.csv",",");
		HashMap<String, HashSet<String>> csvOur = readCSV("ourScore.csv",",");
		System.out.println(calculateAccuracy(csvLabels,csvGiulia));
		System.out.println(calculateAccuracy(csvLabels,csvOur));
		

	}




	/**
	 * this method is used to read a csv file and return HashMap<String,HashSet<String>> where key is a document id and value is a labels list for same document. Replace all space with _
	 * @param pathFile
	 * @param splitType
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String,HashSet<String>> readCSV(String pathFile,String splitType) throws IOException{
		HashMap<String,HashSet<String>> toReturn = new HashMap<String, HashSet<String>>();

		FileReader fileReader = new FileReader(pathFile);
		BufferedReader reader = new BufferedReader(fileReader);

		String line = reader.readLine();
		line = reader.readLine();
		while(line!=null){
			HashSet<String> currentLabels = new HashSet<String>();
			//Se la riga non ha virgole, allora significa che è presente solo l'id
			if(!line.contains(splitType)){
				toReturn.put(line, currentLabels);
			}
			//Altrimenti ci sono le labels
			else{
				String elements[] = line.split(splitType);
				//Ulteriore controllo per vedere se ci sono labels
				if(elements.length>0){
					for(int i=1;i<elements.length;i++){
						//sostituisco gli spazi con _
						currentLabels.add(elements[i].replace(" ", "_"));
					}
				}
				//inserisco nella mappa
				toReturn.put(elements[0], currentLabels);
			}
			line = reader.readLine();
		}
		return toReturn;
	}


	/**
	 * this method return the exact score using two input labels list
	 * @param realLabels
	 * @param labelsToCheck
	 * @return
	 */
	public static float calculateAccuracy(HashMap<String, HashSet<String>> realLabels,HashMap<String, HashSet<String>> labelsToCheck){
		float sum = 0;
		for(String id : labelsToCheck.keySet()){
			HashSet<String> real = realLabels.get(id);
			//per ogni label del documento corrente dei documenti da controllare
			for(String label : labelsToCheck.get(id)){
				//se la lista delle labels reali contiene la label corrente allora aggiungo 1 e interrompo il ciclo, andando al prossimo documento
				if(real.contains(label)){
					sum ++;
					break;
				}
			}
		}
		return sum/labelsToCheck.size();

	}
}

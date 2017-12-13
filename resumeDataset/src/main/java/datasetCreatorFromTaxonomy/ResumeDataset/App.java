package datasetCreatorFromTaxonomy.ResumeDataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class App 
{
	public static void main( String[] args ) throws IOException
	{
		FileReader reader = new FileReader("classes.txt");

		BufferedReader br = new BufferedReader(reader);


		String line = br.readLine();
		while(line!=null){

			String[] splitLine=line.split(";");
			int length = splitLine.length;
			int lengthApp = length;
			new File("C:/Users/Luigi/Desktop/root").mkdir();
			while(lengthApp>1)
			{

				if(lengthApp==5){
					new File("C:/Users/Luigi/Desktop/root/"+splitLine[lengthApp-5]).mkdir();
					
					new File("C:/Users/Luigi/Desktop/1").mkdir();
					new File("C:/Users/Luigi/Desktop/1/"+splitLine[lengthApp-5]).mkdir();
					new File("C:/Users/Luigi/Desktop/1/"+splitLine[lengthApp-5]+"/"+splitLine[lengthApp-4]).mkdir();
					new File("C:/Users/Luigi/Desktop/2").mkdir();
					new File("C:/Users/Luigi/Desktop/2/"+splitLine[lengthApp-4]).mkdir();
					new File("C:/Users/Luigi/Desktop/2/"+splitLine[lengthApp-4]+"/"+splitLine[lengthApp-3]).mkdir();
					new File("C:/Users/Luigi/Desktop/3").mkdir();
					new File("C:/Users/Luigi/Desktop/3/"+splitLine[lengthApp-3]).mkdir();
					new File("C:/Users/Luigi/Desktop/3/"+splitLine[lengthApp-3]+"/"+splitLine[lengthApp-2]).mkdir();
				}
					
				if(lengthApp==4){
					new File("C:/Users/Luigi/Desktop/root/"+splitLine[lengthApp-4]).mkdir();
					new File("C:/Users/Luigi/Desktop/1").mkdir();
					new File("C:/Users/Luigi/Desktop/1/"+splitLine[lengthApp-4]).mkdir();
					new File("C:/Users/Luigi/Desktop/1/"+splitLine[lengthApp-4]+"/"+splitLine[lengthApp-3]).mkdir();
					new File("C:/Users/Luigi/Desktop/2").mkdir();
					new File("C:/Users/Luigi/Desktop/2/"+splitLine[lengthApp-3]).mkdir();
					new File("C:/Users/Luigi/Desktop/2/"+splitLine[lengthApp-3]+"/"+splitLine[lengthApp-2]).mkdir();
				}
				else
					if(lengthApp==3){
						
						new File("C:/Users/Luigi/Desktop/root/"+splitLine[lengthApp-3]).mkdir();
						new File("C:/Users/Luigi/Desktop/1").mkdir();
						new File("C:/Users/Luigi/Desktop/1/"+splitLine[lengthApp-3]).mkdir();
						new File("C:/Users/Luigi/Desktop/1/"+splitLine[lengthApp-3]+"/"+splitLine[lengthApp-2]).mkdir();
					}
					else
						if(lengthApp==2){
							new File("C:/Users/Luigi/Desktop/root/"+splitLine[lengthApp-2]).mkdir();
						}
				lengthApp--;
			}

			line=br.readLine();
		}


	}
}

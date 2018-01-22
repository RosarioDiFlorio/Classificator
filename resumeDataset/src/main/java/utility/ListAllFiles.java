package utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 * Contains some methods to list files and folders from a directory
 *
 * @author Loiane Groner
 * http://loiane.com (Portuguese)
 * http://loianegroner.com (English)
 */
public class ListAllFiles {
    /**
     * List all the files and folders from a directory
     * @param directoryName to be listed
     */
    public void listFilesAndFolders(String directoryName){
        File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList){
            System.out.println(file.getName());
        }
    }
    /**
     * List all the files under a directory
     * @param directoryName to be listed
     */
    public void listFiles(String directoryName){
        File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isFile()){
                System.out.println(file.getName());
            }
        }
    }
    /**
     * List all the folder under a directory
     * @param directoryName to be listed
     */
    public List<String> listFolders(String directoryName){
        File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        List<String> fileList = new ArrayList<String>();
        for (File file : fList){
            if (file.isDirectory()){
                System.out.println(file.getName());
            }
            fileList.add(file.toString());
            
        }
        
        return fileList;
    }
    /**
     * List all files from a directory and its subdirectories
     * @param directoryName to be listed
     * @return 
     */
    public List<String> listFilesAndFilesSubDirectories(String directoryName, List<String> fileList){
        File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isFile()){
                //System.out.println(file.getAbsolutePath());
                fileList.add(file.getAbsolutePath());
            } else if (file.isDirectory()){
                listFilesAndFilesSubDirectories(file.getAbsolutePath(),fileList);
            }
        }
        return fileList;
    }
    public static void main (String[] args){
        ListAllFiles listFilesUtil = new ListAllFiles();
        final String directoryLinuxMac ="/Users/loiane/test";
        //Windows directory example
        final String directoryWindows ="C://Users/Luigi/Desktop";
        //listFilesUtil.listFilesAndFilesSubDirectories(directoryWindows);
        
    }
}
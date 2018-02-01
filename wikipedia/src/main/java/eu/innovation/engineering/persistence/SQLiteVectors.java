package eu.innovation.engineering.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLiteVectors extends SQLiteConnector {


  private  ExecutorService executorService = Executors.newFixedThreadPool(8);

  

  
  public SQLiteVectors(String dbName){
    super(dbName);
    String sql ="CREATE TABLE IF NOT EXISTS vectors (name   VARCHAR PRIMARY KEY NOT NULL, vector BLOB    NOT NULL);";
    try(Statement stm = getConnection().createStatement()){
      stm.executeQuery(sql);
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
 }


  public  void insertVectors(Map<String,float[]> vectors) throws InterruptedException{

    List<InsertTask> list = new ArrayList<InsertTask>();
    for(String name:vectors.keySet()){
      list.add(new InsertTask(super.getConnection(), name, vectors.get(name)));
    }
    executorService.invokeAll(list);
  }



  public  void insertVector(String name,float[] vector) throws SQLException{
    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO vectors VALUES(?,?)");){
      byte[] vectorToSave = fromFloatToByte(vector);
      pstmt.setString(1, name);
      pstmt.setBytes(2, vectorToSave);
      pstmt.executeUpdate();
    }		
  }


  public  float[] getVectorByName(String name){

    String sql = "SELECT vector FROM vectors WHERE name = ?";
    float[] vector = null;
    try (PreparedStatement stm = super.getConnection().prepareStatement(sql)){
      stm.setString(1, name);
      try (ResultSet res = stm.executeQuery()) {
        byte[] byteVector = res.getBytes("vector");
        vector = fromByteToFloat(byteVector); 				
      }

    }catch (Exception e) {
      e.printStackTrace();
    }

    return vector;
  }

  public  boolean isExist(String name){

    String sql = "SELECT name FROM vectors WHERE name =\""+name+"\"";
    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);) {
      res.getString("name").isEmpty();
    }catch (SQLException e) {
      // TODO: handle exception
      return false;
    }
    return true;

  }

  public  Set<String> getNamesVector(){
    Set<String> names = new HashSet<String>();

    String sql = "SELECT name FROM vectors";
    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);){
      while (res.next()) {
        names.add(res.getString("name"));
      }
    }catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    return names;
  }

  public static byte[] fromFloatToByte(float[] array){
    byte buffer[] = new byte[array.length * 4];
    for (int i = 0; i < array.length; i++) {
      int intBits = Float.floatToIntBits(array[i]);
      buffer[i * 4]     = (byte) (((intBits & 0xff000000) >> 24) & 0x000000ff);
      buffer[i * 4 + 1] = (byte) (((intBits & 0x00ff0000) >> 16) & 0x000000ff);
      buffer[i * 4 + 2] = (byte) (((intBits & 0x0000ff00) >> 8) & 0x000000ff);
      buffer[i * 4 + 3] = (byte) (intBits & 0x000000ff);		  
    }
    return buffer;
  }






  public static float[] fromByteToFloat(byte[] buffer) {
    float array[] = null;
    if((buffer.length / 4 ) <=300){
      array = new float[buffer.length / 4];
      for (int i = 0; i < array.length; i++) {
        int intBits = ((buffer[i * 4] & 0x000000ff) << 24) | ((buffer[i * 4 + 1] & 0x000000ff) << 16) | ((buffer[i * 4 + 2] & 0x000000ff) << 8) | (buffer[i * 4 + 3] & 0x000000ff);
        array[i] = Float.intBitsToFloat(intBits);
      }
    }
    else{
      System.err.println(buffer.length);
    }
    return array;
  }

}

class InsertTask implements Callable<Boolean>{

  private String name;
  private float[] vector;
  private Connection conn;



  public InsertTask(Connection conn,String name,float[] vector) {
    this.name = name;
    this.vector = vector;
    this.conn = conn;
  }


  @Override
  public Boolean call() throws Exception {
    try{
      insertVector(name, vector);
    }catch (SQLException e) {
      return false;
    }
    return true;
  }

  private void insertVector(String name,float[] vector) throws SQLException{
    try(PreparedStatement pstmt = conn.prepareStatement("INSERT INTO vectors VALUES(?,?)"); ){
      byte[] vectorToSave = SQLiteVectors.fromFloatToByte(vector);
      pstmt.setString(1, name);
      pstmt.setBytes(2, vectorToSave);
      pstmt.executeUpdate();
    }

  }



}



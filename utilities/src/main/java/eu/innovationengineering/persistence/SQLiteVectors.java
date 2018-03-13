package eu.innovationengineering.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLiteVectors extends SQLiteConnector {

  private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public SQLiteVectors(String dbFolder,String dbName) throws SQLException {
    super(dbFolder + "/"+dbName);
    String sql = "CREATE TABLE IF NOT EXISTS vectors (name   VARCHAR PRIMARY KEY NOT NULL, vector BLOB    NOT NULL);";
    try (Statement stm = getConnection().createStatement()) {
      stm.execute(sql);
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public  void insertVectors(Map<String,float[]> vectors) throws InterruptedException, SQLException{
    for(String label:vectors.keySet()){
      try {
        insertVector(label, vectors.get(label));
      }
      catch (SQLException e) {
        updateVector(label, vectors.get(label));
      }
    }
  }

  public void updateVector(String label, float[] vector) throws SQLException{
    try(PreparedStatement pstmt = super.getConnection().prepareStatement("UPDATE vectors SET vector = ? WHERE name = ?");){
      byte[] vectorToSave = fromFloatToByte(vector);
      pstmt.setBytes(1, vectorToSave);
      pstmt.setString(2, label);
      pstmt.executeUpdate();
    }   
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
      e.printStackTrace();
    }
    return names;
  }


  public Map<String, float[]> getAllVectors(){
    Map<String,float[]> allVectors = new HashMap<String,float[]>();

    String sql = "SELECT * FROM vectors";
    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);){
      while (res.next()) {
        allVectors.put(res.getString("name"),fromByteToFloat(res.getBytes("vector")));
      }
    }catch (Exception e) {
      e.printStackTrace();
    }
    return allVectors;
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



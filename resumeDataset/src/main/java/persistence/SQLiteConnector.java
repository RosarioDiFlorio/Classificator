package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteConnector {
  
  private static Connection conn;
  
  
  public SQLiteConnector(){
    conn = connect();
  }
  
  
  /**Method to establish the connection to the database.
   * @return
   */
  public static Connection connect() {

    // SQLite connection string
    String url = "jdbc:sqlite:D://Development/sqlite/db/databaseVector.db";
    Connection conn = null;
    try {
        conn = DriverManager.getConnection(url);
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return conn;
}
  
  public static void insertVector(String name,float[] vector){
    if(conn == null)
      conn = connect();
    String sql = "INSERT INTO vectors VALUES("+name+","+vector+")";
    try{
      Statement smt = conn.createStatement();
      smt.executeQuery(sql);
    }catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
  }
  
  public static float[] getVectorByName(String name){
    if(conn == null)
      conn = connect();
    String sql = "SELECT vector FROM vectors WHERE name = "+name;
    float[] vector = null;
    try{
      Statement stm = conn.createStatement();
      ResultSet res = stm.executeQuery(sql);
      byte[] byteVector = res.getBytes("vector");
      vector = new float[byteVector.length-1];
      for(int i=0;i<byteVector.length;i++){
        vector[i] = byteVector[i];
      }
      
          }catch (Exception e) {
      // TODO: handle exception
    }
   
    return vector;
  }
  
  
  public static void main(String[] args){
    float[] testVector = new float[1];
    testVector[0] = 1;
    SQLiteConnector.insertVector("ciao", testVector);
  }
  

}

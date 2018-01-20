package persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

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
    String url = "jdbc:sqlite:database.db";
    Connection conn = null;
    try {
        conn = DriverManager.getConnection(url);
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return conn;
}
  
  /**
   * This methos is used to insert values into db
   * @param name
   * @param vector
   * @throws SQLException
   */
  public static void insertVector(String name,float[] vector) throws SQLException{
    if(conn == null)
      conn = connect();
	PreparedStatement pstmt = conn.prepareStatement("INSERT INTO vectors VALUES(?,?)");
	
	
	byte[] vectorToSave = fromFloatToByte(vector);
	pstmt.setString(1, name);
	pstmt.setBytes(2, vectorToSave);
	pstmt.executeUpdate();
   
    
  }
  
  /**
   * This method return a vector by name param. 
   * @param name
   * @return
   */
  public static float[] getVectorByName(String name){
    if(conn == null)
      conn = connect();
    String sql = "SELECT vector FROM vectors WHERE name = '"+name+"'";
    float[] vector = null;
    try{
      Statement stm = conn.createStatement();
      ResultSet res = stm.executeQuery(sql);
      byte[] byteVector = res.getBytes("vector");
      vector = fromByteToFloat(byteVector); 
      
          }catch (Exception e) {
        	  e.printStackTrace();
    }
   
    return vector;
  }
  
  
  /**
   * This method transform a float array into byte array
   * @param array
   * @return
   */
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
  
  
  /**
   * This method transform a byte array into float array
   * @param buffer
   * @return
   */
  public static float[] fromByteToFloat(byte[] buffer) {
	  float array[] = new float[buffer.length / 4];
	  for (int i = 0; i < array.length; i++) {
		  int intBits = ((buffer[i * 4] & 0x000000ff) << 24) | ((buffer[i * 4 + 1] & 0x000000ff) << 16) | ((buffer[i * 4 + 2] & 0x000000ff) << 8) | (buffer[i * 4 + 3] & 0x000000ff);
		  array[i] = Float.intBitsToFloat(intBits);
	  }
	  return array;
  }
  
  

}

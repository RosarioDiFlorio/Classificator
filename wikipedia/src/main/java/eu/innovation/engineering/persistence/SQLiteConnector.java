package eu.innovation.engineering.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class SQLiteConnector {

  private  String urlDb = "jdbc:sqlite:";
  private  Connection conn;



  public SQLiteConnector(String urlDb){
    this.urlDb += urlDb;
    this.conn = connect();

  }


  /**Method to establish the connection to the database.
   * Default autoCommit is false.
   * @return
   */
  public  Connection connect() {

    Connection conn = null;
    try {
      conn = DriverManager.getConnection(urlDb);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return conn;
  }

  /**Default connection is with autoCommit false.
   * @return
   */
  public  Connection getConnection(){
    if(conn == null)
      conn = connect();
    return conn;  
  }

  
  public  void setAutoCommit(boolean isAuto){
    try {
      conn.setAutoCommit(isAuto);
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public  void commitConnection(){
    try {
      conn.commit();
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }




}

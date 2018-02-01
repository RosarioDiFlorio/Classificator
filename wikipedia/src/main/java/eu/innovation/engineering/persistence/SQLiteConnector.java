package eu.innovation.engineering.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.sqlite.JDBC;

public abstract class SQLiteConnector {

  static {
    JDBC driver = new JDBC();
    driver.getMajorVersion();
  }
  
  private  Connection conn;

  protected SQLiteConnector(String urlDb) throws SQLException{
    this.conn = DriverManager.getConnection("jdbc:sqlite:" + urlDb);
  }

  /**Default connection is with autoCommit false.
   * @return
   */
  public  Connection getConnection(){
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

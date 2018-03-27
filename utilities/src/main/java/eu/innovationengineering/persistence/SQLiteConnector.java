package eu.innovationengineering.persistence;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import org.sqlite.JDBC;

public abstract class SQLiteConnector implements Closeable {

  static {
    JDBC driver = new JDBC();
    driver.getMajorVersion();
  }

  private  Connection conn;

  protected SQLiteConnector(String urlDb) throws SQLException{
    if(!new File(urlDb).exists())
      try {
        new File(urlDb).createNewFile();
      }
    catch (IOException e) {
      e.printStackTrace();
    }
    this.conn = DriverManager.getConnection("jdbc:sqlite:" + urlDb);
  }

  @Override
  public void close() throws IOException {
    try {
      conn.close();

      Enumeration<Driver> drivers = DriverManager.getDrivers();
      while (drivers.hasMoreElements()) {
        Driver driver = drivers.nextElement();
        if (driver instanceof JDBC) {
          DriverManager.deregisterDriver(driver);
          break;
        }
      }
    }
    catch (SQLException e) {
      throw new IOException(e);
    }
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

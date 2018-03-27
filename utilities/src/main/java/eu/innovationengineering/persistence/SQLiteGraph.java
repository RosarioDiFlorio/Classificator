package eu.innovationengineering.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.innovationengineering.utilities.Result;





public class SQLiteGraph extends SQLiteConnector  {



  public SQLiteGraph (String dbFolder,String dbName) throws SQLException {
    super(dbFolder + "/"+dbName);
    String edgesTable = "CREATE TABLE IF NOT EXISTS  edges (   from  VARCHAR NOT NULL, to VARCHAR NOT NULL, value DOUBLE  NOT NULL, PRIMARY KEY (from,to) );";

    try(Statement stm = getConnection().createStatement()){
      stm.executeQuery(edgesTable);
    }
    catch (SQLException e) {
    } 
  }


  public List<Edge> getListEdgesByValue(double value){
    List<Edge> toReturn = new ArrayList<Edge>();
    String sql = "SELECT * FROM edges WHERE value= ? ;";
    try(PreparedStatement pstm = getConnection().prepareStatement(sql);){
      pstm.setDouble(1, value);
      try (ResultSet res = pstm.executeQuery()) {
        while (res.next()) {
          toReturn.add(new Edge(res.getString("from"), res.getString("to"), res.getDouble("value")));
        }
      }
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
    return toReturn;

  }


  public Set<String> getLabelsFromEdges(){
    Set<String> toReturn = new HashSet<String>();
    String sql = "SELECT from, to FROM edges";
    try(PreparedStatement stm = getConnection().prepareStatement(sql)){
      try (ResultSet res = stm.executeQuery()) {
        while (res.next()) {
          toReturn.add(res.getString("from"));
          toReturn.add(res.getString("to"));
        }
      }
    }catch (SQLException e) {
      e.printStackTrace();
    }   
    return toReturn;
  }

  
  
  public void insertGraph(Map<String,List<Result>> graph) throws SQLException{
    for(String from: graph.keySet()){
      insertEdges(from, graph.get(from));
    }
  }
  

  
  

  public Map<String,List<Result>> getGraph(String typeLinked){
    System.out.println("Inizialing graph "+typeLinked);
    Map<String,List<Result>> toReturn = new HashMap<String, List<Result>>();
    String sql = "SELECT * FROM edges";
    try(PreparedStatement stm = getConnection().prepareStatement(sql)){
      try (ResultSet res = stm.executeQuery()) {
        while (res.next()) {
          Result v = new Result(res.getString(typeLinked), res.getDouble("value"));          
          String key = null;
          List<Result> linkedVertex = null;      

          if(typeLinked.equals("from"))
            key = res.getString("to");
          else
            key = res.getString("from");


          if(toReturn.containsKey(key)){
            linkedVertex = toReturn.get(key);
            linkedVertex.add(v);
            toReturn.replace(key, linkedVertex);
          }else{
            linkedVertex = new ArrayList<Result>();
            linkedVertex.add(v);
            toReturn.put(key, linkedVertex);
          }

        }
      }
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("graph loaded -> "+toReturn.size());
    return toReturn;
  }





  /**
   * method used to insert edge into db
   * @param from
   * @param to
   * @param value
   * @throws SQLException
   */
  public void insertEdge(String from, String to, double value) throws SQLException{
    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO edges VALUES(?,?,?)");){	
      pstmt.setString(1, from);
      pstmt.setString(2, to);
      pstmt.setDouble(3, value);
      pstmt.executeUpdate();
    }
  }




  /**
   * method used to insert an edge list into db
   * @param from
   * @param destination
   * @param weight
   * @throws SQLException
   */
  public void insertEdges(String from, List<Result> nodeResults) throws SQLException{

    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO edges VALUES(?,?,?)");){	
      pstmt.setString(1, from);
      for(Result res : nodeResults){
        pstmt.setString(2, res.getLabel());
        pstmt.setDouble(3, res.getValue());
        pstmt.executeUpdate();
      }
    }
  }


  public void updateEdge(String source,String dest,double value){
    String sql = "UPDATE edges SET value = ? WHERE from = ? AND to = ?";
    try(PreparedStatement pstmt = super.getConnection().prepareStatement(sql);){
      pstmt.setDouble(1, value);
      pstmt.setString(2, source);
      pstmt.setString(3, dest);
      pstmt.executeUpdate();
    }
    catch (SQLException e) {
      e.printStackTrace();
    } 
  }

  public void updateEdges(String from, List<Result> nodeResults) throws SQLException{
    String sql = "UPDATE edges SET value = ? WHERE from = ? AND to = ?";
    try(PreparedStatement pstmt = super.getConnection().prepareStatement(sql);){  
      pstmt.setString(1, from);
      for(Result res : nodeResults){
        pstmt.setString(2, res.getLabel());
        pstmt.setDouble(3, res.getValue());
        pstmt.executeUpdate();
      }
    }
  }
  

  /**
   * return query results. 
   * @param from
   * @param queryType. Type of query. "to" if you would a childList, "from" if you would a parentList
   * @return
   * @throws SQLException
   */
  public NodeResult getEdgeList(String from, String queryType) throws SQLException{

    List<Result> vertexList = new ArrayList<Result>();
    // build sql request 
    String sql=null;
    if(queryType.equals("from"))
      sql = "SELECT from,value FROM edges WHERE to =\""+from+"\"";
    else if(queryType.equals("to"))
      sql = "SELECT to,value FROM edges WHERE from =\""+from+"\"";


    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);){
      boolean result = res.next();


      //while there are row in result to read
      while(result){
        String dest = res.getString(queryType);
        double value = res.getDouble("value");
        vertexList.add(new Result(dest,value));
        result = res.next();
      }
    }
    return new NodeResult(from, vertexList);
  }

}


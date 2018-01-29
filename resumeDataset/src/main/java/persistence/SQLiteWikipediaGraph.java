package persistence;

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

import utility.Vertex;

public class SQLiteWikipediaGraph extends SQLiteConnector  {



  public Map<String,EdgeResult> getGraph(String typeLinked){
    System.out.println("Inizialing graph");
    Map<String,EdgeResult> toReturn = new HashMap<String, EdgeResult>();
    String sql = "SELECT * FROM edges";
    try(PreparedStatement stm = getConnection().prepareStatement(sql)){
      try (ResultSet res = stm.executeQuery()) {
        while (res.next()) {
          Vertex v = new Vertex(res.getString(typeLinked), res.getDouble("distance"));          
          String key = null;
          List<Vertex> linkedVertex = null;      
          EdgeResult toUpdate = null;
          if(typeLinked.equals("parents"))
            key = res.getString("childs");
          else
            key = res.getString("parents");

          
          if(toReturn.containsKey(key)){
            toUpdate = toReturn.get(key);
            linkedVertex = toUpdate.getLinkedVertex();
            linkedVertex.add(v);
            toUpdate.setLinkedVertex(linkedVertex);
            toReturn.replace(key, toUpdate);
          }else{
            linkedVertex = new ArrayList<Vertex>();
            linkedVertex.add(v);
            toUpdate = new EdgeResult(key, linkedVertex);
            toReturn.put(key, toUpdate);
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





  public SQLiteWikipediaGraph (String dbName){
    super(dbName);
    String markedNodesTable ="CREATE TABLE IF NOT EXISTS  markedNodes (  name   VARCHAR (255) PRIMARY KEY NOT NULL, marked BOOLEAN NOT NULL);";
    String edgesTable = "CREATE TABLE IF NOT EXISTS  edges (   parents  VARCHAR NOT NULL, childs VARCHAR NOT NULL, distance DOUBLE  NOT NULL, PRIMARY KEY (parents,childs) );";

    try(Statement stm = getConnection().createStatement()){
      stm.executeQuery(markedNodesTable);  
    }
    catch (SQLException e) {
    }
    try(Statement stm = getConnection().createStatement()){
      stm.executeQuery(edgesTable);
    }
    catch (SQLException e) {
    } 
  }


  /**
   * method used to insert edge into db
   * @param source
   * @param destination
   * @param weight
   * @throws SQLException
   */
  public void insertEdge(String source, String destination, double weight) throws SQLException{


    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO edges VALUES(?,?,?)");){	

      pstmt.setString(1, source);
      pstmt.setString(2, destination);
      pstmt.setDouble(3, weight);
      pstmt.executeUpdate();
    }
  }
  
  

  /**
   * method used to insert an edge list into db
   * @param source
   * @param destination
   * @param weight
   * @throws SQLException
   */
  public void insertEdgeList(String source, List<String> destination, List<Double> weight) throws SQLException{

    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO edges VALUES(?,?,?)");){	
      pstmt.setString(1, source);
      for(int i=0;i<destination.size();i++){
        pstmt.setString(2, destination.get(i));
        pstmt.setDouble(3, weight.get(i));
        pstmt.executeUpdate();
      }
    }
  }


  public void insertMarkedNode(String name, boolean isMarked){
    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO markedNodes VALUES(?,?)");){
      pstmt.setString(1, name);
      pstmt.setBoolean(2, isMarked);
      pstmt.executeUpdate();
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public boolean isMarked(String name){
    String sql = "SELECT marked FROM markedNodes WHERE name =\""+name+"\"";

    boolean toReturn = false;
    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);) {
      toReturn = res.getBoolean("marked");
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return toReturn;
  }

  /**
   * return query results. 
   * @param source
   * @param queryType. Type of query. "childs" if you would a childList, "parents" if you would a parentList
   * @return
   * @throws SQLException
   */
  public EdgeResult getEdgeList(String source, String queryType) throws SQLException{

    List<Vertex> vertexList = new ArrayList<Vertex>();
    // build sql request 
    String sql=null;
    if(queryType.equals("parents"))
      sql = "SELECT parents,distance FROM edges WHERE childs =\""+source+"\"";
    else if(queryType.equals("childs"))
      sql = "SELECT childs,distance FROM edges WHERE parents =\""+source+"\"";


    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);){
      boolean result = res.next();


      //while there are row in result to read
      while(result){
        String dest = res.getString(queryType);
        double distance = res.getDouble("distance");
        vertexList.add(new Vertex(dest,distance));
        result = res.next();
      }
    }
    return new EdgeResult(source, vertexList);
  }

  public Set<String> getMarkedNodes(){
    Set<String> names = new HashSet<>();
    String sql ="SELECT name FROM markedNodes WHERE  marked = ?";
    try(PreparedStatement stm = getConnection().prepareStatement(sql)){
      stm.setBoolean(1, true);
      try (ResultSet res = stm.executeQuery()) {
        while (res.next()) {
          names.add(res.getString("name"));
        }
      }
    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return names;
  }


  public boolean isExistMarked(String name){
    String sql = "SELECT name FROM markedNodes WHERE name= ? ;";
    try(PreparedStatement pstm = getConnection().prepareStatement(sql);){
      pstm.setString(1, name);
      try (ResultSet res = pstm.executeQuery()) {
        if(res.getString("name").isEmpty())
          return false;
        else return true;
      }


    }
    catch (SQLException e) {
      // TODO Auto-generated catch block
      return false;
    }

  }


  public void updateMarkedNode(String name,boolean value){
    String sql = "UPDATE markedNodes SET marked = ? WHERE name = ?";
    try(PreparedStatement pstmt = super.getConnection().prepareStatement(sql);){
      pstmt.setBoolean(1, value);
      pstmt.setString(2, name);
      pstmt.executeUpdate();
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void insertAndUpdateMarkedNodes(Set<String> toMark){
    setAutoCommit(false);
    Set<String> alreadyMarked = getMarkedNodes();
    alreadyMarked.remove(toMark);
    alreadyMarked.forEach(el->updateMarkedNode(el, false));
    for(String name: toMark){
      if(isExistMarked(name)){
        updateMarkedNode(name, true);
      }else{
        insertMarkedNode(name, true);
      }
    }
    commitConnection();
    setAutoCommit(true);
  }

  public Set<String> getNameNodes(){
    Set<String> names = new HashSet<>();
    String sql = "SELECT name FROM markedNodes";
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
}


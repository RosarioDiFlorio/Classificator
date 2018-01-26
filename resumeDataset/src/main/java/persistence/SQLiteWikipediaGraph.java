package persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utility.Vertex;

public class SQLiteWikipediaGraph extends SQLiteConnector  {



  public SQLiteWikipediaGraph (String dbName){
    super(dbName);
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


  public void insertMarkedNode(String name, boolean isMarked) throws SQLException{
    try(PreparedStatement pstmt = super.getConnection().prepareStatement("INSERT INTO markedNodes VALUES(?,?)");){
      pstmt.setString(1, name);
      pstmt.setBoolean(2, isMarked);
      pstmt.executeUpdate();
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
      sql = "SELECT parents,weight FROM edges WHERE childs =\""+source+"\"";
    else if(queryType.equals("childs"))
      sql = "SELECT childs,weight FROM edges WHERE parents =\""+source+"\"";


    try(Statement stm = super.getConnection().createStatement();
        ResultSet res = stm.executeQuery(sql);){
      boolean result = res.next();


      //while there are row in result to read
      while(result){
        String dest = res.getString(queryType);
        double distance = res.getDouble("weight");
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
    Set<String> alreadyMarked = getMarkedNodes();
    toMark.forEach(el->updateMarkedNode(el, true));
    alreadyMarked.removeAll(toMark);
    alreadyMarked.forEach(el->updateMarkedNode(el, false));
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

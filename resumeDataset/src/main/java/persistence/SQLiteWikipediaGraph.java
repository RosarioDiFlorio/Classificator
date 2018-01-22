package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import utility.Vertex;

public class SQLiteWikipediaGraph extends SQLiteConnector  {

	private static Connection conn;
	
	
	public SQLiteWikipediaGraph (String dbName){
		
		super(dbName);
		conn = getConnection();
		
	}


	/**
	 * method used to insert edge into db
	 * @param source
	 * @param destination
	 * @param weight
	 * @throws SQLException
	 */
	public void insertEdge(String source, String destination, double weight) throws SQLException{

		if(conn == null)
			conn = connect();

		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO edge VALUES(?,?,?)");	

		pstmt.setString(1, source);
		pstmt.setString(2, destination);
		pstmt.setDouble(3, weight);
		pstmt.executeUpdate();

	}

	/**
	 * method used to insert an edge list into db
	 * @param source
	 * @param destination
	 * @param weight
	 * @throws SQLException
	 */
	public void insertEdgeList(String source, List<String> destination, List<Double> weight) throws SQLException{

		if(conn == null)
			conn = connect();

		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO edge VALUES(?,?,?)");	

		pstmt.setString(1, source);
		for(int i=0;i<destination.size();i++){
			pstmt.setString(2, destination.get(i));
			pstmt.setDouble(3, weight.get(i));
			pstmt.executeUpdate();
		}

	}

	/**
	 * return query results. 
	 * @param source
	 * @param queryType. Type of query. "childs" if you would a childList, "parents" if you would a parentList
	 * @return
	 * @throws SQLException
	 */
	public EdgeResult getEdgeList(String source, String queryType) throws SQLException{
		if(conn == null)
			conn = connect();
		
		// build sql request 
		String sql=null;
		if(queryType.equals("parents"))
			sql = "SELECT parents,weight FROM edge WHERE childs = '"+source+"'";
		else if(queryType.equals("childs"))
			sql = "SELECT childs,weight FROM edge WHERE parents = '"+source+"'";

		
		 Statement stm = conn.createStatement();
		 stm.executeQuery(sql);
		 ResultSet res = stm.executeQuery(sql);
		 boolean result = res.next();
		 List<Vertex> vertexList = new ArrayList<Vertex>();
		 
		 //while there are row in result to read
		 while(result){
			 String dest = res.getString(queryType);
			 double distance = res.getDouble("weight");
			 vertexList.add(new Vertex(dest,distance));
			 result = res.next();
		 }
		 
		 return new EdgeResult(source, vertexList);

	}





}

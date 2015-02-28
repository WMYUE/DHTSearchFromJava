package com.konka.dhtsearch.db.mongodb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

import com.konka.dhtsearch.db.mysql.exception.DhtException;
import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * @author 耳东 (cgp@0731life.com)
 *
 */
public class MongodbConnectionProvider {

	private String driver = "com.mysql.jdbc.Driver";
	private String dbName = "dht";
	// private String passwrod = "";
	private String passwrod = "cgp888";
	private String userName = "root";
	private String url = "jdbc:mysql://localhost:3306/" + dbName + "?useUnicode=true&characterEncoding=UTF-8";

	// -----------------------------------------------------------------
	private static MongodbConnectionProvider instance;
	private Connection connection = null;

	private MongodbConnectionProvider() throws DhtException {
		try {
			Class.forName(driver);
			connection = DriverManager.getConnection(url, userName, passwrod);
			// DriverManager.getConnection(url, info)
			// Properties d=new Properties();
			// d.s

		} catch (SQLException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static MongodbConnectionProvider getInstance() throws DhtException {
		if (instance == null) {
			instance = new MongodbConnectionProvider();
		}
		 
		return instance;
	}
	  public static void main( String args[] ){
	      try{   
			 // 连接到 mongodb 服务
	         Mongo mongoClient = new Mongo( "localhost" , 27017 );
	         
	         // 连接到数据库
	         DB db = mongoClient.getDB( "test" );
	         
			 System.out.println("Connect to database successfully");
			 Set<String> colls = db.getCollectionNames();
//	         boolean auth = db.authenticate(myUserName, myPassword);
//			 System.out.println("Authentication: "+auth);
	      }catch(Exception e){
		     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		  }
	   }
	 

}

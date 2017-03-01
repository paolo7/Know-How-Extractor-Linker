package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.hp.hpl.jena.query.QuerySolution;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

public class DatabaseConnectorMySQL3 implements DatabaseConnector{

    private String framework = "embedded";
    private String driver = "com.mysql.jdbc.Driver";
    
    private String protocol = "jdbc:mysql://localhost:3306/";
    private String dbName = "integrationDataFast"; 
    private Connection conn = null;
    private Statement statement = null;
    private int batchInsertOrder = 0;
	public DatabaseConnectorMySQL3(){
		
	}
	
	@Override
	public void storeMap(String synset, String source, String entity) {
		storeMap(synset, entity);
	}
	
	public void storeMap(String synset, String entity) {
		if(conn == null){
			System.out.println("DATA - ERROR: before storing data it is necessary to open the connection");
		} else {
			try {
				statement.executeUpdate("insert into "+sanitiseSynsetName(synset)+" values (\""+entity+"\")");
				}
	            //System.out.println("Inserted row: ["+synset+", "+source+", "+entity+"]");
			 catch(SQLIntegrityConstraintViolationException e) {
				// do nothing, as this is just a duplicated row
			}
			catch (MySQLSyntaxErrorException e) {
				if(createTable(synset));
					storeMap(synset, entity);
			}
			catch (SQLException e) {
				System.out.println("DATA - ERROR: failed to store a row in the synsetmap");
				e.printStackTrace();
			}
			
		}
	}
	
	public ResultSet getAllTables(){
		if(conn == null){
			System.out.println("DATA - ERROR: before retrieving table names it is necessary to open the connection");
		} else {
				try {
					return statement.executeQuery("select * from information_schema.tables");
				} catch (SQLException e) {
					System.out.println("DATA - ERROR: failed retrieving table names");
					e.printStackTrace();
				}
		}
		return null;
	}
	
	public ResultSet getAllEntitiesInTable(String tableName){
		if(conn == null){
			System.out.println("DATA - ERROR: before retrieving entities in table it it necessary to open the connection");
		} else {
				try {
					return statement.executeQuery("select entity from "+tableName);
				} catch (SQLException e) {
					System.out.println("DATA - ERROR: failed retrieving entities in table");
					e.printStackTrace();
				}
		}
		return null;
	}
	
	public int countAllEntitiesInTable(String tableName){
		if(conn == null){
			System.out.println("DATA - ERROR: before retrieving entities in table it it necessary to open the connection");
		} else {
				try {
					ResultSet set = statement.executeQuery("SELECT COUNT(*) FROM "+tableName);
					if(set == null) return -1;
					while(set.next()){
						return set.getInt(1);
					}
				} catch(SQLException e){
					e.printStackTrace();
				}
					
		}
		return -1;
	}
	
	public void forceCommit(){
		try {
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean createTable(String synset) {
		if(conn == null){
			System.out.println("DATA - ERROR: before storing data it is necessary to open the connection");
		} else {
			try {
				statement.executeUpdate("create table "+sanitiseSynsetName(synset)+"(entity varchar(767))");// UNIQUE NOT NULL
				System.out.print("|");
				//System.out.println("DATA - Created table "+sanitiseSynsetName(synset));
				return true;
				}
			catch (SQLException e) {
				System.out.println("DATA - ERROR: failed to create table "+sanitiseSynsetName(synset));
				e.printStackTrace();
			} 
		}
		return false;
	}
	
	public String sanitiseSynsetName(String synset){
		return synset.replaceAll("-", "AxA");
	}
	
	/*			INSERT INTO TwoColumnTable VALUES
    (1, 'first row'),
    (2, 'second row'),
    (3, 'third row')*/
	@Override
	public void open() {
		loadDriver();
		Properties props = new Properties(); 
		try {
            props.put("user", "root");
            props.put("password", "");
			// Connect to the database (and create it if it does not exist)
			conn = DriverManager.getConnection(protocol, props);
			Statement statement = conn.createStatement();
			System.out.println("Connecting to DB");
			//int myResult = statement.executeUpdate("DROP DATABASE "+dbName+";");
			int myResult = statement.executeUpdate("CREATE DATABASE IF NOT EXISTS "+dbName+";");
			System.out.println("Connected to DB");
			statement.close();
			conn.close();
			conn = DriverManager.getConnection(protocol + dbName, props);
			System.out.println("DATA - Connected to database " + dbName);
			conn.setAutoCommit(false);
			this.statement = conn.createStatement();
			
			
			/*//Add unique to tables
			System.out.println("Updating uniqueness started");			
			this.statement.execute("	select concat(\" alter table \", a.table_name, \" ADD UNIQUE ( 'entity' ) \") "
					+ " from information_schema.tables a "
					+ "where a.table_name like '%';");
			System.out.println("Updating uniqueness finished");
			// stop
			conn = null;
			return;*/
			
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	@Override
	public void close() {
		try
        {
            // the shutdown=true attribute shuts down Derby
            DriverManager.getConnection("jdbc:derby:;shutdown=true");

            // To shut down a specific database only, but keep the
            // engine running (for example for connecting to other
            // databases), specify a database in the connection URL:
            //DriverManager.getConnection("jdbc:derby:" + dbName + ";shutdown=true");
        }
        catch (SQLException se)
        {
            if (( (se.getErrorCode() == 50000)
                    && ("XJ015".equals(se.getSQLState()) ))) {
                // we got the expected exception
                System.out.println("Derby shut down normally");
                // Note that for single database shutdown, the expected
                // SQL state is "08006", and the error code is 45000.
            } else {
                // if the error code or SQLState is different, we have
                // an unexpected exception (shutdown failed)
                System.err.println("Derby did not shut down normally");
                printSQLException(se);
            }
        }
	}
	
	/**
     * Loads the appropriate JDBC driver for this environment/framework. For
     * example, if we are in an embedded environment, we load Derby's
     * embedded Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
     */
    private void loadDriver() {
        try {
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("\nUnable to load the JDBC driver " + driver);
            System.err.println("Please check your CLASSPATH.");
            cnfe.printStackTrace(System.err);
        } catch (InstantiationException ie) {
            System.err.println(
                        "\nUnable to instantiate the JDBC driver " + driver);
            ie.printStackTrace(System.err);
        } catch (IllegalAccessException iae) {
            System.err.println(
                        "\nNot allowed to access the JDBC driver " + driver);
            iae.printStackTrace(System.err);
        }
    }
    
    public static void printSQLException(SQLException e)
    {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null)
        {
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            //e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }

	@Override
	public void dropTable(String tableName) {
		// TODO Auto-generated method stub
		
	}


}

package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnectorMySQL implements DatabaseConnector{

    private String framework = "embedded";
    private String driver = "com.mysql.jdbc.Driver";
    
    private String protocol = "jdbc:mysql://localhost:3306/";
    private String dbName = "integrationData"; 
    private Connection conn = null;
    private PreparedStatement psInsert = null;
    private int batchInsertOrder = 0;
    private Statement s;
    
	public DatabaseConnectorMySQL(){
		
	}
	@Override
	public void storeMap(String synset, String source, String entity) {
		if(conn == null){
			System.out.println("DATA - ERROR: before storing data it is necessary to open the connection");
		} else {
			
			
			try {
				System.out.println("insert into "+synset+" value("+entity+")");
				s.execute("insert into "+synset+" value("+entity+")");
				System.out.print("+");
			} catch( SQLException e ) {
				if(e.getSQLState().equals("42S01")) {
					
				  }
				else {
					System.out.println("DATA - ERROR while storing synset occurrence");
					printSQLException(e);
				}
		    }	
			
			
		}
		
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
			int myResult = statement.executeUpdate("CREATE DATABASE IF NOT EXISTS "+dbName+";");
			conn.close();
			conn = DriverManager.getConnection(protocol + dbName, props);
			System.out.println("DATA - Connected to database " + dbName);
			conn.setAutoCommit(false);
			// Create table if it does not exist
			s = conn.createStatement();
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
    public void forceCommit(){
		try {
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public void storeMap(String synset, String entity) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ResultSet getAllTables() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ResultSet getAllEntitiesInTable(String tableName) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int countAllEntitiesInTable(String tableName) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void dropTable(String tableName) {
		// TODO Auto-generated method stub
		
	}


}

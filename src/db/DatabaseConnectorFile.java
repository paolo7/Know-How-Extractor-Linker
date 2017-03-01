package db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

public class DatabaseConnectorFile implements DatabaseConnector{

    private String pathToDataDirectory = "/home/paolo/Projects/DataDirectory/TexFilesSynsetMap/";
    
	public DatabaseConnectorFile(){
		
	}
	
	@Override
	public void storeMap(String synset, String source, String entity) {
		storeMap(synset, entity);
	}
	
	public void storeMap(String synset, String entity) {
		try {
			File yourFile = new File(pathToDataDirectory+synset);
			if(!yourFile.exists()){
				yourFile.createNewFile();
			}
			FileOutputStream outputStream = new FileOutputStream(pathToDataDirectory+synset, true);
			outputStream.write((entity+"\n").getBytes("UTF-8"));
			outputStream.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void forceCommit(){
		
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

	}
	@Override
	public void close() {
		
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

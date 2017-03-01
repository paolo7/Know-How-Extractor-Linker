package db;

import java.sql.ResultSet;

public interface DatabaseConnector {

	public void forceCommit();
	public void open();
	public void close();
	public void storeMap(String synset, String source, String entity);
	public void storeMap(String synset, String entity);
	public ResultSet getAllTables();
	public ResultSet getAllEntitiesInTable(String tableName);
	public int countAllEntitiesInTable(String tableName);
	public void dropTable(String tableName);
}

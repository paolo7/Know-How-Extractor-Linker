package dbpediaLinks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class SimpleLogger implements Logger{

	public static void logFile(String s) throws IOException{
		FileWriter writer = new FileWriter("logPrint.txt", true);
		writer.write(s + "\n");
		writer.flush();
		writer.close();
	}

	@Override
	public void log(String s){
		Date now = new Date();
		System.out.println("["+now+"] "+s);
	}
	
	@Override
	public void log(int i, String s){
		Date now = new Date();
		System.out.println("["+now+"] "+s);
	}

}

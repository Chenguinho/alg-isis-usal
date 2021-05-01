package classes;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class FileLog {
	
	String logFileSend, logFileMail;
	
	/*
	public FileLog(String logFolder, String path1, String path2) {
		
		File folder = new File(logFolder);
		File fichero1 = new File(path1);
		File fichero2 = new File(path2);
		
		try {
			
			if(!folder.exists()) 
				folder.mkdirs();
			
			if(!fichero1.exists())
				fichero1.createNewFile();
			
			if(!fichero2.exists())
				fichero2.createNewFile();
			
		} catch (Exception e) {
			System.out.println("ERROR: Creación de fichero");
			e.printStackTrace();
		}
		
	}
	*/
	
	public FileLog(String logFolder, String pathToLogFileSend, String pathToLogFileMail) {
		
		logFileSend = pathToLogFileSend;
		logFileMail = pathToLogFileMail;
		
		File folder = new File(logFolder);
		File fichero1 = new File(pathToLogFileSend);
		File fichero2 = new File(pathToLogFileMail);
		
		try {
			
			if(!folder.exists()) 
				folder.mkdirs();
			
			if(!fichero1.exists())
				fichero1.createNewFile();
			
			if(!fichero2.exists())
				fichero2.createNewFile();
			
		} catch (Exception e) {
			System.out.println("ERROR: Creación de fichero");
			e.printStackTrace();
		}
		
	}
	
	public void log(String pathToFile, String logMsg) {
		
		FileWriter logFile = null;
		PrintWriter pw = null;
		
		try {
			
			logFile = new FileWriter(pathToFile);
			pw = new PrintWriter(logFile);
			
			pw.println(logMsg);
			
			pw.close();
			
		} catch (Exception e) {
			
			System.out.println("ERROR: Escritura de archivo");
			e.printStackTrace();
			
		}
		
	}
	
	public String GetSend() {
		return this.logFileSend;
	}
	
	public String GetMail() {
		return this.logFileMail;
	}
	
}

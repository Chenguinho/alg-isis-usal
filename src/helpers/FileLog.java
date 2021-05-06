package helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class FileLog {
	
	String fileName;
	
	public FileLog(String dir, Integer idProceso) {
		
		this.fileName = dir + idProceso + "log.txt";
		
		File folder = new File(dir);
		File file = new File(fileName);
		
		try {
			
			if(!folder.exists())
				folder.mkdirs();
			
			if(file.exists())
				file.delete();
			
			file.createNewFile();
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	public void log(String pathToFile, String logMsg) {
		
		FileWriter logFile = null;
		PrintWriter pw = null;
		
		try {
			
			logFile = new FileWriter(pathToFile, true);
			pw = new PrintWriter(logFile);
			
			pw.println(logMsg);
			
			pw.close();
			
		} catch (Exception e) {
			
			System.out.println("ERROR: Escritura de archivo");
			e.printStackTrace();
			
		}
		
	}
	
	public String GetFileName() {
		
		return this.fileName;
		
	}
	
}

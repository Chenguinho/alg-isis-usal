package helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

/*
 * La clase de apoyo FileLog se encarga de crear lo necesario
 * para poder escribir el log del proceso, es decir, crear la carpeta
 * en el directorio home del usuario si es que no existe y "reiniciar"
 * los ficheros de log.
 * Tambien se encarga de la propia escritura del fichero log.
 */

public class FileLog {
	
	String fileName;
	
	/*
	 * Creacion FileLog
	 * 
	 * Comprueba si existe el directorio (lo creara si no existe)
	 * Comprueba si existe el fichero log (lo eliminara y creara otro si existe,
	 * si no, simplemente lo creara)
	 */
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
	
	
	//Metodo para escribir en el fichero log
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
	
	/*
	 * Metodo para contar las lineas del fichero log (empleado para 
	 * controlar el final del proceso)
	 */
	public int CountLines() {
		
		try {
			
			File file = new File(this.fileName);
			Scanner sc = new Scanner(file);
			int contador = 0;
			
			while(sc.hasNextLine()) {
				sc.nextLine();
				contador++;
			}
			
			sc.close();
			
			return contador;
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		return 0;
		
	}
	
	//Metodo GET
	
	public String GetFileName() {
		
		return this.fileName;
		
	}
	
}

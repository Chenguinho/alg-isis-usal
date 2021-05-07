package helpers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Diff {

	List<String> ficheros;
	
	int contador = 0;
	
	boolean exitLoop;
	
	int resultado;
	
	public Diff() {
		
		ficheros = new ArrayList<String>();
		
	}
	
	public void AddToList(String f) {
		
		this.ficheros.add(f);
		
	}
	
	public void Check() throws IOException, FileNotFoundException {
		
		List<FileReader> fileReaders = new ArrayList<FileReader>();
		List<BufferedReader> bufferedReaders = new ArrayList<BufferedReader>();
		
		String comp = "";
		resultado = 1;
		exitLoop = false;
		
		for(int i = 0; i < ficheros.size(); i++) {
			
			FileReader f = new FileReader(ficheros.get(i));
			BufferedReader b = new BufferedReader(f);
			
			fileReaders.add(f);
			bufferedReaders.add(b);
			
		}
		
		while(!exitLoop && (comp = bufferedReaders.get(0).readLine()) != null) {
			
			for(int i = 1; i < bufferedReaders.size(); i++) {
				
				if(bufferedReaders.get(i).readLine().compareToIgnoreCase(comp) != 0 && contador != 0) {
					
					exitLoop = true;
					resultado = -1;
	
				}
				
				contador++;
				
			}
			
		}
		
		for(int i = 0; i < bufferedReaders.size(); i++) {
			
			bufferedReaders.get(i).close();
			
		}
		
		if(resultado != -1) {
			
			System.out.println("CORRECTO");
			
		} else {
			
			System.out.println("UNA MIERDA");
			
		}
		
	}
	
}

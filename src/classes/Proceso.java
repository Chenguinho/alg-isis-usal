package classes;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import isis.Multi;

public class Proceso extends Thread {

	Mail mailService;
	Multi multiService;
	
	FileLog logger;
	
	int idProceso, idEquipo;
	
	String ipEquipo;
	
	List<String> vecinos;
	
	//Constructor
	public Proceso(int idProceso, int idEquipo, List<String> vecinos, String ip, FileLog logger) {
		this.idProceso = idProceso;
		this.idEquipo = idEquipo;
		this.ipEquipo = ip;
		this.vecinos = vecinos;
		
		mailService = new Mail();
		multiService = new Multi();
		
		this.logger = logger;	
	}
	
	//Métodos constructor
	
	//SET -> Guardar valores
	
	public void SetIdProceso(int i) {
		idProceso = i;
	}
	
	public void SetIdEquipo(int i) {
		idEquipo = i;
	}
	
	public void SetVecinos(List<String> v) {
		vecinos = v;
	}
	
	//GET -> Obtener valores
	
	public int GetIdProceso() {
		return idProceso;
	}
	
	public int GetIdEquipo() {
		return idEquipo;
	}
	
	public List<String> GetVecinos(){
		return vecinos;
	}
	
	//Método run() del hilo
	
	public void run() {
		String second = String.format("%02d", LocalDateTime.now().getSecond());
		String instant = LocalDateTime.now().getHour() + 
				":" + LocalDateTime.now().getMinute() + 
				":" + second;
		
		logger.log(logger.GetSend(), "Proceso " + idProceso + " a las " + instant);
		logger.log(logger.GetMail(), "Proceso " + idProceso + " a las " +instant);
		
		/*
		 * TODO
		 * 
		 * 1 - Crear un semáforo con tantos "huecos" como procesos haya
		 * 2 - Cada vez que se haga un run() de un proceso hacer un acquire del semaforo
		 * 3 - Cuando se hagan todos, seguir
		 * 
		 */
		
		NotifyCreated();
		
	}
	
	void NotifyCreated() {
		
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://" + ipEquipo + ":8080/practicaFinal/isis").build();
		WebTarget target = client.target(uri);
		
		target.path("wait").request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
}

package classes;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import isis.Multicast;

public class Proceso extends Thread {

	private Sleep sleep = new Sleep();
	
	Mail mailService;
	
	FileLog logger;
	
	int idProceso, idEquipo;
	
	String ipEquipo, ipServer;
	
	List<String> vecinos;
	List<Message> mensajes;
	
	public Semaphore semControlMulticast;
	
	//Constructor
	public Proceso(int idProceso, int idEquipo, List<String> vecinos, String ip, FileLog logger, String ipServer) {
		this.idProceso = idProceso;
		this.idEquipo = idEquipo;
		this.ipEquipo = ip;
		this.vecinos = vecinos;
		this.mensajes = new ArrayList<Message>();
		this.ipServer = ipServer;
		
		mailService = new Mail();
		
		this.logger = logger;
		
		semControlMulticast = new Semaphore(0);
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
		
		NotifyCreated();
		
		String instant = String.format("%02d", LocalDateTime.now().getHour()) + 
				":" + String.format("%02d", LocalDateTime.now().getMinute()) + 
				":" + String.format("%02d", LocalDateTime.now().getSecond());
		
		logger.log(logger.GetSend(), "Proceso " + idProceso + " a las " + instant);
		logger.log(logger.GetMail(), "Proceso " + idProceso + " a las " +instant);
		
		for(int i = 1; i < 101; i++) {
			
			Date d = new Date();
			Message m = new Message(i, idEquipo, idProceso, d.getTime(), 0);
			
			mensajes.add(m);
			
			Multicast multicast = new Multicast(m, semControlMulticast, ipEquipo, logger);
			multicast.start();
			
			try {
				semControlMulticast.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	void NotifyCreated() {
		
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://" + ipEquipo + ":8080/practicaFinal/isis").build();
		WebTarget target = client.target(uri);
		
		target.path("waitForProcs").request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
}

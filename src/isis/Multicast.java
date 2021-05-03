package isis;

import java.net.URI;
import java.util.concurrent.Semaphore;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import classes.FileLog;
import classes.Message;
import classes.Sleep;

public class Multicast extends Thread {

	private Sleep sleep = new Sleep();
	
	Message mensaje;
	Semaphore semControlMulticast;
	String ipServer;
	FileLog logger;
	
	private final int MAXPROCESOS = Isis.MAXPROCESOS;
	
	public Multicast(Message m, Semaphore sem, String ip, FileLog log) {
		mensaje = m;
		ipServer = ip;
		semControlMulticast = sem;
		logger = log;
	}
	
	public void run() {
		
		for(int i = 0; i < MAXPROCESOS; i++) {
			
			logger.log(logger.GetSend(), mensaje.GetContent());
			
			Client client = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + ipServer + ":8080/practicaFinal/isis").build();
			WebTarget target = client.target(uri);
			
			target.path("multicastMsg")
				.queryParam("content", mensaje.GetContent())
				.queryParam("ipServer", ipServer)
				.queryParam("idDest", i + 1)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			
			sleep.ThreadSleep(0.2f, 0.5f);
			
		}
		
		semControlMulticast.release();
		
	}
	
	
	
}

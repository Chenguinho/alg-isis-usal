package classes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import isis.Isis;
import isis.Multicast;

public class Proceso extends Thread {

	private Sleep sleep = new Sleep();
	private Network network = new Network();
	
	Mail mailService;
	
	FileLog logger;
	
	int idProceso, idEquipo;
	
	String ipEquipo, ipServer;
	
	List<String> vecinos;
	List<Message> mensajes;
	
	private Semaphore semControlMulticast;
	
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
		
		semControlMulticast = new Semaphore(1);
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
		
		//for(int j = 0; j < Isis.MAXPROCESOS; j++) {
			
			for(int i = 1; i < 10; i++) {
				
				try {
					semControlMulticast.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				Date d = new Date();
				Message m = new Message(i, idEquipo, idProceso, d.getTime(), 0);
				
				mensajes.add(m);
				
				Multicast multicast = new Multicast(m, semControlMulticast, ipEquipo, logger);
				multicast.start();
				
				sleep.ThreadSleep(1f, 1.5f);
				
			}
			
		//}
		
	}
	
	public void receiveMulticast(Message m, Integer origen) {
		
		System.out.println("RECIBIDO MULTICAST | " + m.GetContent() + " en proceso " + idProceso);
		
		Date d = new Date();
		
		m.SetOrder(d.getTime());
		
		mailService.buzon.add(m);
		
		SendPurpose(m, origen);
		
	}
	
	public void receivePurpose(Message m) {
		
		System.out.println("PROPUESTA RECIBIDA | " + m.GetContent() + " de proceso " + m.GetProcess() + " en proceso " + idProceso);
		
		Date d = new Date();
		Message msg = new Message();
		
		if(m.GetOrder() < d.getTime())
			m.SetOrder(d.getTime());
		
		for(int i = 0; i < mailService.GetBuzon().size(); i++) {
			
			if(m.GetId() == mailService.GetBuzon().get(i).GetId() && m.GetProcess() == mailService.GetBuzon().get(i).GetProcess()) {
				msg = mailService.GetBuzon().get(i);
			}
			
		}
		
		if(m.GetOrder() > msg.GetOrder())
			msg.SetOrder(m.GetOrder());
		
		msg.SetPropuestas(msg.GetPropuestas() + 1);	
		
		System.out.println("Nueva propuesta recibida para " + msg.GetContent() + " -> " + msg.GetPropuestas());
		
		if(msg.GetPropuestas() == Isis.MAXPROCESOS) {
			
			msg.SetStatus(1);
			SendMultiDef(msg);
			
		}
		
	}
	
	void NotifyCreated() {
		
		WebTarget target = network.CreateClient(ipEquipo);
		
		target.path("waitForProcs").request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	void SendPurpose(Message m, Integer origen) {
		
		WebTarget target = network.CreateClient(ipEquipo);
		
		target.path("sendPurpose")
			.queryParam("content", m.GetContent())
			.queryParam("id", m.GetId())
			.queryParam("idEquipo", m.GetComputer())
			.queryParam("idProcMen", m.GetProcess())
			.queryParam("idProceso", idProceso)
			.queryParam("order", m.GetOrder())
			.queryParam("ipServer", ipServer)
			.queryParam("idDest", origen)
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	void SendMultiDef(Message m) {
		
		WebTarget target = network.CreateClient(ipEquipo);
		
		target.path("sendMultiDef")
			.queryParam("content", m.GetContent())
			.queryParam("id", m.GetId())
			.queryParam("idEquipo", m.GetComputer())
			.queryParam("idProceso", m.GetProcess())
			.queryParam("order", m.GetOrder())
			.queryParam("ipServer", ipServer)
			.queryParam("propuestas", m.GetPropuestas())
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
}

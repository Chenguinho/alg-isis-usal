package classes;

import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import isis.Isis;

public class Proceso extends Thread {

	//Constantes
	
	private static final String LOGFOLDER = System.getProperty("user.home") + "/isis/";
	private static final String LOGSEND = "LogSend.txt";
	private static final String LOGMAIL = "LogMail.txt";
	
	//Clases con metodos de ayuda
	
	private Network network = new Network();
	private Sleep sleep = new Sleep();
	
	//Atributos proceso
	
	Integer idProceso, idEquipo;
	
	String ipServer, ipCentral;
	
	Buzon buzon;
	
	FileLog fileLog;
	
	private Integer ordenProceso;
	
	//Semaforos
	
	static Semaphore semControlOrder = new Semaphore(1);
	static Semaphore semControlBuzon = new Semaphore(1);
	static Semaphore semControlFin = new Semaphore(0);
	
	//Control del final
	
	static int contadorProcesosFinal;
	
	//Constructor
	
	public Proceso(Integer idP, Integer idE, String ip) {
		
		this.idProceso = idP;
		this.idEquipo = idE;
		
		this.ipServer = ip;
		
		buzon = new Buzon();
		
		fileLog = new FileLog(LOGFOLDER, idP + LOGSEND, idP + LOGMAIL);
		
	}
	
	//Metodo run() del hilo (Proceso.start())
	
	public void run() {
		
		NotifyCreated();
		
		String tStart = String.format("%02d", LocalDateTime.now().getHour())
						+ ":" +
						String.format("%02d", LocalDateTime.now().getMinute())
						+ ":" +
						String.format("%02d", LocalDateTime.now().getSecond());
		
		for(int i = 0; i < 10; i++) {
			
			Message m = new Message(i + 1, idEquipo, idProceso, 0, 0, 0);
			
			for(int j = 0; j < Isis.MAXPROCESOS; j++) {
				
				WebTarget target = network.CreateClient(ipServer);
				
				target.path("multicastMsg")
					.queryParam("idMensaje", m.GetIdMensaje())
					.queryParam("idProceso", m.GetIdProceso())
					.queryParam("idEquipo", m.GetIdEquipo())
					.request(MediaType.TEXT_PLAIN).get(String.class);
				
				sleep.ThreadSleep(0.2f, 0.5f);
				
			}
			
			sleep.ThreadSleep(1.0f, 1.5f);
			
		}
		
	}
	
	//Recepcion del primer mensaje multicast
	
	public void receiveMulticast(Message m, Integer idOrigen) {
		
		try{
			
			semControlOrder.acquire();
			
			m.SetOrdenLC1();
			ordenProceso = m.GetOrden();
			
			semControlOrder.release();
			
			buzon.AddMessage(m);
			
			SendPropuesta(m, idOrigen);
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion de la propuesta del mensaje multicast
	
	public void receivePropuesta(Message m) {
		
		try {
			
			semControlOrder.acquire();
			
			m.SetOrdenLC2(ordenProceso);
			ordenProceso = m.GetOrden();
			
			Message mensajeBuzon = buzon.GetMessage(m.GetIdMensaje(), m.GetIdProceso());
			
			MaxOrden(mensajeBuzon, m);
			
			mensajeBuzon.SetPropuestas(mensajeBuzon.GetPropuestas() + 1);
			
			semControlOrder.release();
			
			if(mensajeBuzon.GetPropuestas() == Isis.MAXPROCESOS) {
				
				mensajeBuzon.SetEstado(1);
				SendAcuerdo(mensajeBuzon);
				
			}
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion del acuerdo
	
	public void receiveAcuerdo(Message m) {
		
		try {
			
			semControlOrder.acquire();
			
			m.SetOrdenLC2(ordenProceso);
			ordenProceso = m.GetOrden();
			
			Message mensajeBuzon = buzon.GetMessage(m.GetIdMensaje(), m.GetIdProceso());
			
			MaxOrden(mensajeBuzon, m);
			
			mensajeBuzon.SetEstado(1);
			
			if(buzon.GetBuzonLength() >= 2)
				buzon.Order();
			
			semControlOrder.release();
			
			/*
			 * TODO
			 * 
			 * ULTIMA PARTE
			 * 
			 * LOG Y ESAS COSITAS
			 * Arreglar el FileLog porque no ecribe :(
			 * 
			 */
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	/*
	 * Metodo para comprobar que mensake tiene el
	 * mayor orden y almacenar en el primero (que
	 * sera el que obtengamos del buzon) el valor
	 * mas alto
	 */
	
	void MaxOrden(Message m1, Message m2) {
		
		if(m1.GetOrden() < m2.GetOrden())
			m1.SetOrden(m2.GetOrden());
		
	}
	
	/*
	 * Metodo para avisar al servidor de que hemos
	 * creado un proceso mas y esperar a que todos
	 * esten creados para continuar
	 */
	
	void NotifyCreated() {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("waitForProcs")
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
		
	}
	
	//Enviar la propuesta al proceso
	
	void SendPropuesta(Message m, Integer idOrigen) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("sendPropuesta")
			.queryParam("idMensaje", m.GetIdMensaje())
			.queryParam("idProceso", m.GetIdProceso())
			.queryParam("idEquipo", m.GetIdEquipo())
			.queryParam("orden", m.GetOrden())
			.queryParam("idEquipoDestino", idOrigen)
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Enviar el acuerdo
	
	void SendAcuerdo(Message m) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("sendAcuerdo")
			.queryParam("idMensaje", m.GetIdMensaje())
			.queryParam("idProceso", m.GetIdProceso())
			.queryParam("idEquipo", m.GetIdEquipo())
			.queryParam("orden", m.GetOrden())
			.queryParam("numPropuestas", m.GetPropuestas())
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Metodos GET
	
	public Integer GetIdProceso() {
		
		return this.idProceso;
		
	}
	
}
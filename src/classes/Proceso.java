package classes;

import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import helpers.Diff;
import helpers.FileLog;
import helpers.Network;
import helpers.Sleep;
import isis.Isis;

public class Proceso extends Thread {

	//Constantes
	
	private static final String FOLDER = System.getProperty("user.home") + "/isis/";
	
	//Clases con metodos de ayuda
	
	private Network network = new Network();
	private Sleep sleep = new Sleep();
	private static Diff d = new Diff();
	
	//Atributos proceso
	
	Integer idProceso, idEquipo;
	
	String ipServer, ipCentral;
	
	Buzon buzon;
	
	FileLog fileLog;
	
	//Tiempo logico de Lamport (clock)
	
	Integer ordenProceso = 0;
	
	//Flag para abandonar el bucle de escritura
	
	boolean exitLoop = false;
	
	//Semaforos
	
	private Semaphore semControlBuzon = new Semaphore(1);
	private Semaphore semControlOrden = new Semaphore(1);
	
	//Constructor
	
	public Proceso(Integer idP, Integer idE, String ip) {
		
		this.idProceso = idP;
		this.idEquipo = idE;
		
		this.ipServer = ip;
		
		buzon = new Buzon();
		
		fileLog = new FileLog(FOLDER, idP);
		d.AddToList(fileLog.GetFileName());
		
	}
	
	//Metodo run() del hilo (Proceso.start())
	
	public void run() {
		
		System.out.println("PROCESO " + idProceso + " CREADO");
		
		NotifyCreated();
		
		for(int i = 0; i < 5; i++) {
			
			Message m = new Message(i + 1, idEquipo, idProceso, 0, 0, 0);
			
			try {
				
				semControlBuzon.acquire();
				
				buzon.AddMessage(m);
				
				semControlBuzon.release();
				
			} catch(InterruptedException e) {
				
				e.printStackTrace();
				
			}
			
			for(int j = 0; j < Isis.MAXPROCESOS; j++) {
				
				WebTarget target = network.CreateClient(ipServer);
				
				target.path("multicastMsg")
					.queryParam("idMensaje", m.GetIdMensaje())
					.queryParam("idProceso", m.GetIdProceso())
					.queryParam("idEquipo", m.GetIdEquipo())
					.queryParam("idDestino", j + 1)
					.request(MediaType.TEXT_PLAIN).get(String.class);
				
				sleep.ThreadSleep(0.2f, 0.5f);
				
			}
			
			sleep.ThreadSleep(1.0f, 1.5f);
			
		}
		
	}
	
	//Recepcion del primer mensaje multicast
	
	public void receiveMulticast(Message m, Integer idOrigen) {

		try {
			
			semControlOrden.acquire();
			
			m.SetOrdenLC1();
			ordenProceso = m.GetOrden();
			
			semControlOrden.release();
			
			if(m.GetIdProceso() != idProceso) {
				
				semControlBuzon.acquire();
				buzon.AddMessage(m);
				semControlBuzon.release();
				
			} else {
				
				semControlBuzon.acquire();
				
				Message msg = buzon.GetMessage(m);
				msg.SetOrden(m.GetOrden());
				buzon.DeleteMessage(msg);
				buzon.AddMessage(msg);
				
				semControlBuzon.release();
				
			}
			
			SendPropuesta(m, idOrigen);
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion de la propuesta del mensaje multicast
	
	public void receivePropuesta(Message m) {
		
		try {
			
			semControlOrden.acquire();
			
			m.SetOrdenLC2(ordenProceso);
			ordenProceso = m.GetOrden();
			
			semControlOrden.release();
			
			semControlBuzon.acquire();
			
			Message mensajeBuzon = buzon.GetMessage(m);
			
			if(mensajeBuzon.GetOrden() < m.GetOrden())
				mensajeBuzon.SetOrden(m.GetOrden());
			
			mensajeBuzon.SetPropuestas(mensajeBuzon.GetPropuestas() + 1);
			
			if(mensajeBuzon.GetPropuestas() == Isis.MAXPROCESOS) {			
				
				mensajeBuzon.SetEstado(1);
				
				buzon.DeleteMessage(mensajeBuzon);
				buzon.AddMessage(mensajeBuzon);
				
				semControlBuzon.release();
				
				SendAcuerdo(mensajeBuzon);
				
			} else {
				
				buzon.AddMessage(mensajeBuzon);
				
				semControlBuzon.release();
				
			}
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion del acuerdo
	
	public void receiveAcuerdo(Message m) {
		
		try {
			
			//Tiempo logico de Lamport
			semControlOrden.acquire();
			
			m.SetOrdenLC2(ordenProceso);
			ordenProceso = m.GetOrden();
			
			semControlOrden.release();
			
			//Obtengo el mensaje del buzon con el id correspondiente
			semControlBuzon.acquire();
			
			Message mensajeBuzon = buzon.GetMessage(m);
			
			//Se hacen las comprobaciones y modificaciones necesarias
			
			if(mensajeBuzon.GetOrden() < m.GetOrden())
				mensajeBuzon.SetOrden(m.GetOrden());
			
			mensajeBuzon.SetPropuestas(m.GetPropuestas());
			mensajeBuzon.SetEstado(1);
			mensajeBuzon.SetAgreed(true);
			
			//Se vuelve a almacenar en el buzon
			buzon.DeleteMessage(mensajeBuzon);
			buzon.AddMessage(mensajeBuzon);
			
			//Ordenamos el buzon (ORDEN DESC)
			
			if(buzon.GetBuzonLength() >= 2)
				buzon.Order();
			
			semControlBuzon.release();
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
		if(buzon.GetBuzonLength() == 10)
			ImprimirBuzon();
		
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
	
	void SendPropuesta(Message m, Integer idDestino) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("sendPropuesta")
			.queryParam("idMensaje", m.GetIdMensaje())
			.queryParam("idProceso", m.GetIdProceso())
			.queryParam("idEquipo", m.GetIdEquipo())
			.queryParam("orden", m.GetOrden())
			.queryParam("idDestino", idDestino)
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
	
	void ImprimirBuzon() {
		
		System.out.println(" | BUZON PROC " + idProceso);
		for(int i = 0; i < buzon.GetBuzonLength(); i++) {
			
			System.out.println(
				" | " + 
				buzon.GetBuzonList().get(i).GetContenido() +
				" | " + 
				String.format("%3d", buzon.GetBuzonList().get(i).GetOrden()) + 
				" | " +
				String.format("%2d", buzon.GetBuzonList().get(i).GetPropuestas()) +
				" | " +
				String.format("%1d", buzon.GetBuzonList().get(i).GetEstado()) +
				" |");
			
		}
		System.out.println();
		
	}
	
	void FinalSend() {
		
		if(!buzon.empty()) {
			
			exitLoop = false;
			
			Message msg = buzon.GetFirst();
			while(!exitLoop) {
				
				if(msg.GetEstado() == 0 || !msg.GetAgreed()) {
					
					exitLoop = true;
					
				} else {
					
					fileLog.log(fileLog.GetFileName(), msg.GetContenido());
					buzon.RemoveFirst();
					
					if(!buzon.empty()) {
						
						msg = buzon.GetFirst();
						
					} else {
						
						exitLoop = true;
						
					}
					
				}
				
			}
			
		}
		
	}
	
	void Debug(String funcion, Message m) {
		
		System.out.println(" " + idProceso + " | " + funcion + " -> " + m.GetContenido());
		
	}
	
	//Metodos GET
	
	public Integer GetIdProceso() {
		
		return this.idProceso;
		
	}
	
}
package classes;

import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

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
	
	//Atributos proceso
	
	Integer idProceso, idEquipo;
	
	String ipServer, ipCentral;
	
	Buzon buzon;
	
	FileLog fileLog;
	
	//Tiempo logico de Lamport (clock)
	
	private Integer ordenProceso;
	
	//Variables
	
	boolean exitLoop = false; //Flag para abandonar el bucle de escritura
	static int finProc = 0;
	
	//Semaforos
	
	private static Semaphore semControlOrden = new Semaphore(1);
	private static Semaphore semControlLineas = new Semaphore(1);
	
	//Constructor
	
	public Proceso(Integer idP, Integer idE, String ip) {
		
		this.idProceso = idP;
		this.idEquipo = idE;
		
		this.ipServer = ip;
		
		buzon = new Buzon();
		
		fileLog = new FileLog(FOLDER, idP);
		
		this.ordenProceso = 0;
		
	}
	
	//Metodo run() del hilo (Proceso.start())
	
	public void run() {
		
		System.out.println("PROCESO " + idProceso + " CREADO");
		
		NotifyCreated();
		
		for(int i = 0; i < Isis.NUMMENSAJES; i++) {
			
			try {
				
				semControlOrden.acquire();
				
			} catch (InterruptedException e) {
				
				e.printStackTrace();
				
			}
			
			Message m = new Message(i + 1, idProceso, 0, 0, 0);
			buzon.AddMessage(m);
			
			semControlOrden.release();
			
			for(int j = 0; j < Isis.MAXPROCESOS; j++) {
				
				WebTarget target = network.CreateClient(ipServer);
				
				target.path("multicastMsg")
					.queryParam("idMensaje", m.GetIdMensaje())
					.queryParam("idProceso", m.GetIdProceso())
					.queryParam("idDestino", j + 1)
					.request(MediaType.TEXT_PLAIN).get(String.class);
				
				sleep.ThreadSleep(0.2f, 0.5f);
				
			}
			
			sleep.ThreadSleep(1.0f, 1.5f);
			
		}
		
	}
	
	//Recepcion del primer mensaje multicast
	
	public void receiveMulticast(Integer idM, Integer idP, Integer idOrigen, Integer ordered) {

		if(ordered == 1) {
			
			try {
				
				semControlOrden.acquire();
				
				ordenProceso = LC1(ordenProceso);
				
				if(idP != idProceso) {
				
					Message m = new Message(idM, idP, ordenProceso, 0, 0);
					buzon.AddMessage(m);
					
				} else {
					
					buzon.GetMessage(idM, idP).SetOrden(ordenProceso);
					
				}
				
				semControlOrden.release();
				
				SendPropuesta(idM, idP, ordenProceso, idOrigen, idProceso);
				
			} catch (InterruptedException e) {
				
				e.printStackTrace();
				
			}
			
		} else {
			
			try {
				
				semControlOrden.acquire();
				
				Message m = new Message(idM, idP, 0, 0, 0);
				
				fileLog.log(fileLog.GetFileName(), m.GetContenido());
				
				semControlOrden.release();
				
				ControlFin2();
			
			} catch (InterruptedException e) {
				
				e.printStackTrace();
				
			}
				
		}
			
	}
		
		
	
	//Recepcion de la propuesta del mensaje multicast
	
	public void receivePropuesta(Integer idM, Integer idP, Integer orden, Integer idDesempate) {
		
		try {
			
			semControlOrden.acquire();
			
			ordenProceso = LC2(ordenProceso, orden);
			
			if(buzon.GetMessage(idM, idP).GetOrden() < orden) {
				
				buzon.GetMessage(idM, idP).SetOrden(orden);
				
			}
			
			Integer numPropuestas = buzon.GetMessage(idM, idP).GetPropuestas() + 1;
			buzon.GetMessage(idM, idP).SetPropuestas(numPropuestas);
			
			if(buzon.GetMessage(idM, idP).GetPropuestas() == Isis.MAXPROCESOS) {
				
				buzon.GetMessage(idM, idP).SetEstado(1);
				
				semControlOrden.release();
				
				SendAcuerdo(idM, 
							idP, 
							buzon.GetMessage(idM, idP).GetOrden(), 
							buzon.GetMessage(idM, idP).GetPropuestas(),
							idDesempate);
				
			} else {
				
				semControlOrden.release();
				
			}
				
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion del acuerdo
	
	public void receiveAcuerdo(Integer idM, Integer idP, Integer orden, Integer propuestas, Integer idDesempate) {
		
		try {
			
			semControlOrden.acquire();
			
			ordenProceso = LC2(ordenProceso, orden);
			
			buzon.GetMessage(idM, idP).SetOrden(orden);
			buzon.GetMessage(idM, idP).SetPropuestas(propuestas);
			buzon.GetMessage(idM, idP).SetEstado(1);
			
			if(buzon.GetBuzonLength() > 1)
				buzon.Order(idDesempate);
			
			while(!buzon.empty() && buzon.GetFirst().GetEstado() == 1) {
				
				fileLog.log(fileLog.GetFileName(),
						" | " +
						buzon.GetFirst().GetContenido() 
						+ " | " +
						buzon.GetFirst().GetOrden() 
						+ " | ");
				buzon.RemoveFirst();
				
			}
			
			semControlOrden.release();
			
			ControlFin();
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	void ControlFin() {
		
		Integer lineas = fileLog.CountLines();
		
		try {
			
			semControlLineas.acquire();
			
			if(lineas == Isis.MAXPROCESOS * Isis.NUMMENSAJES) {
				
				semControlLineas.release();
				CheckLogs();
				
			} else {
				
				semControlLineas.release();
				
			}
			
		} catch(InterruptedException e) {
			
			e.printStackTrace();
			
		}		
		
	}
	
	void ControlFin2() {
		
		Integer lineas = fileLog.CountLines();
		
		try {
			
			semControlLineas.acquire();
			if(lineas == Isis.MAXPROCESOS * Isis.NUMMENSAJES) {
				
				semControlLineas.release();
				CheckLogs2();
				
			} else {
				
				semControlLineas.release();
			} 
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Llamadas al servidor...
	
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
	
	void SendPropuesta(Integer idM, Integer idP, Integer orden, Integer idDestino, Integer idDesempate) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("sendPropuesta")
			.queryParam("idMensaje", idM)
			.queryParam("idProceso", idP)
			.queryParam("orden", orden)
			.queryParam("idDestino", idDestino)
			.queryParam("idDesempate", idDesempate)
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Enviar el acuerdo
	
	void SendAcuerdo(Integer idM, Integer idP, Integer orden, Integer prop, Integer idDesempate) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		for(int j = 0; j < Isis.MAXPROCESOS; j++) {
			
			target.path("sendAcuerdo")
				.queryParam("idMensaje", idM)
				.queryParam("idProceso", idP)
				.queryParam("orden", orden)
				.queryParam("numPropuestas", prop)
				.queryParam("idDesempate", idDesempate)
				.queryParam("destino", j + 1)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			
		}
		
	}
	
	//Llamada a servidor para comprobacion de logs
	
	void CheckLogs() {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("checkLogs")
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	void CheckLogs2() {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("checkLogs2")
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Metodos para controlar los tiempos logicos
	
	public Integer LC1(Integer orden) {
		
		return orden + 1;
		
	}
	
	public Integer LC2(Integer timestamp, Integer ordenMensaje) {
		
		Integer returnValue;
		
		if(timestamp >= ordenMensaje) {
			
			returnValue = timestamp + 1;
			
		} else {
			
			returnValue = ordenMensaje + 1;
			
		}
		
		return returnValue;
		
	}
	
	//Metodos GET
	
	public Integer GetIdProceso() {
		
		return this.idProceso;
		
	}
	
	public FileLog GetFileLog() {
		
		return this.fileLog;
		
	}
	
	/*
	 * Metodos para depuracion de errores y tratar de buscar
	 * sus respectivas soluciones.
	 * ImprimirBuzon() imprime el buzon de cada proceso imprimiendo
	 * su contenido, su orden, su numero de propuestas y su estado (1 si es 
	 * definitivo o 0 si es provisional).
	 * Debug(String, Message) recibe como parametro una cadena que
	 * se utiliza para saber en que fase del programa estamos depurando
	 * y un mensaje para imprimir informacion sobre el
	 */
	
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
	
	void Debug(String funcion, Message m, Integer destino) {
		
		System.out.println(" " + idProceso + " | " + funcion + " -> " + m.GetContenido() + " | " + m.GetOrden() + " -> " + destino);
		
	}
	
}
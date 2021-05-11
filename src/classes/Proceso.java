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
	private static Semaphore semControlFin1 = new Semaphore(1);
	
	private static Semaphore semControlFin = new Semaphore(0);
	
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
	
	public void receiveMulticast(Integer idM, Integer idP, Integer idOrigen) {

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
			
			SendPropuesta(idM, idP, ordenProceso, idOrigen);
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion de la propuesta del mensaje multicast
	
	public void receivePropuesta(Integer idM, Integer idP, Integer orden) {
		
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
				
				SendAcuerdo(idM, idP, buzon.GetMessage(idM, idP).GetOrden(), buzon.GetMessage(idM, idP).GetPropuestas());
				
			} else {
				
				semControlOrden.release();
				
			}
				
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion del acuerdo
	
	public void receiveAcuerdo(Integer idM, Integer idP, Integer orden, Integer propuestas) {
		
		try {
			
			semControlOrden.acquire();
			
			ordenProceso = LC2(ordenProceso, orden);
			
			buzon.GetMessage(idM, idP).SetOrden(orden);
			buzon.GetMessage(idM, idP).SetPropuestas(propuestas);
			buzon.GetMessage(idM, idP).SetEstado(1);
			
			if(buzon.GetBuzonLength() > 1) {
				buzon.Order();
			}
			
			//ImprimirBuzon();
			
			while(!buzon.empty() && buzon.GetFirst().GetEstado() == 1) {
				
				buzon.Order();
				fileLog.log(fileLog.GetFileName(), buzon.GetFirst().GetContenido() + " | " + buzon.GetFirst().GetOrden());
				buzon.RemoveFirst();
				
			}
			
			ControlFin();
			
			//semControlOrden.release();
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
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
	
	void SendPropuesta(Integer idM, Integer idP, Integer orden, Integer idDestino) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("sendPropuesta")
			.queryParam("idMensaje", idM)
			.queryParam("idProceso", idP)
			.queryParam("orden", orden)
			.queryParam("idDestino", idDestino)
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Enviar el acuerdo
	
	void SendAcuerdo(Integer idM, Integer idP, Integer orden, Integer prop) {
		
		WebTarget target = network.CreateClient(ipServer);
		
		for(int j = 0; j < Isis.MAXPROCESOS; j++) {
			
			//Debug("SEND ACUERDO", buzon.GetMessage(idM, idP), j + 1);
			
			target.path("sendAcuerdo")
				.queryParam("idMensaje", idM)
				.queryParam("idProceso", idP)
				.queryParam("orden", orden)
				.queryParam("numPropuestas", prop)
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
	
	/*
	 * Esta funcion es la que se encarga de la parte final del 
	 * programa, es decir, se encarga de ir borrando los mensajes
	 * del buzon mientras va escribiendo en el fichero log que 
	 * utilizamos para simular el envio e ir obteniendo el siguiente
	 * mensaje mientras el estado de este sea 1 (definitivo).
	 */
	
	void FinalSend() {
		
		if(!buzon.empty()) {
			
			exitLoop = false;
			
			while(!exitLoop) {
				
				if(buzon.GetFirst().GetEstado() == 0) {
					
					exitLoop = true;
					
				} else {
					
					fileLog.log(fileLog.GetFileName(), buzon.GetFirst().GetContenido());
					buzon.RemoveFirst();
					
					if(buzon.empty()) {
						
						exitLoop = true;
						
					}
					
				}
				
			}
			
		}
		
	}
	
	/*
	 * Esta funcion es la que se encarga de controlar si el programa
	 * ha finalizado. Lo hace a través de una llamada a una funcion en la
	 * clase FileLog que se encarga basicamente de contar el numero de
	 * lineas del fichero log, si esta tiene tantas como mensajes tiene
	 * que haber, el proceso ha terminado y espera a los demas.
	 */
	
	void ControlFin() {
		
		if(Isis.MAXPROCESOS * Isis.NUMMENSAJES == fileLog.CountLines()) {
			
			try {
				
				semControlFin1.acquire();
				
				finProc++;
				
				if(finProc == Isis.MAXPROCESOS) {
					
					finProc = 0;
					
					semControlFin1.release();
					semControlOrden.release();
					semControlFin.release(Isis.MAXPROCESOS - 1);
					
					CheckLogs();
					
				} else {
					
					semControlFin1.release();
					semControlOrden.release();
					semControlFin.acquire();
					
				}
				
			} catch (InterruptedException e) {
				
				e.printStackTrace();
				
			}
			
		} else {
			
			semControlOrden.release();
			
		}
		
	}
	
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
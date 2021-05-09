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
	
	Integer ordenProceso = 0;
	
	//Variables
	
	boolean exitLoop = false; //Flag para abandonar el bucle de escritura
	static int fin = 0; //Contador fin
	static int finProc = 0;
	
	//Semaforos
	
	private static Semaphore semControlOrden = new Semaphore(1);
	private static Semaphore semControlFin = new Semaphore(0);
	
	//Constructor
	
	public Proceso(Integer idP, Integer idE, String ip) {
		
		this.idProceso = idP;
		this.idEquipo = idE;
		
		this.ipServer = ip;
		
		buzon = new Buzon();
		
		fileLog = new FileLog(FOLDER, idP);
		
	}
	
	//Metodo run() del hilo (Proceso.start())
	
	public void run() {
		
		System.out.println("PROCESO " + idProceso + " CREADO");
		
		NotifyCreated();
		
		for(int i = 0; i < Isis.NUMMENSAJES; i++) {
			
			Message m = new Message(i + 1, idEquipo, idProceso, 0, 0, 0);
			
			buzon.AddMessage(m);
			
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
			
			if(m.GetIdProceso() != idProceso) {
			
				buzon.AddMessage(m);
				
			} else {
				
				Message msg = buzon.GetMessage(m);
				msg.SetOrden(m.GetOrden());
				
			}
			
			semControlOrden.release();
			
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
			
			Message mensajeBuzon = buzon.GetMessage(m);
			
			if(mensajeBuzon.GetOrden() < m.GetOrden())
				mensajeBuzon.SetOrden(m.GetOrden());
			
			mensajeBuzon.SetPropuestas(mensajeBuzon.GetPropuestas() + 1);
			
			if(mensajeBuzon.GetPropuestas() == Isis.MAXPROCESOS) {
				
				mensajeBuzon.SetEstado(1);
				
				semControlOrden.release();
				
				SendAcuerdo(mensajeBuzon);
				
			} else {
				
				semControlOrden.release();
				
			}
				
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion del acuerdo
	
	public void receiveAcuerdo(Message m) {
		
		try {

			semControlOrden.acquire();
			
			m.SetOrdenLC2(ordenProceso);
			ordenProceso = m.GetOrden();
			
			Message mensajeBuzon = buzon.GetMessage(m);
			
			mensajeBuzon.SetOrden(m.GetOrden());
			mensajeBuzon.SetPropuestas(m.numPropuestas);
			mensajeBuzon.SetEstado(1);
			
			if(buzon.GetBuzonLength() > 1)
				buzon.Order();
			
			FinalSend();
			
			ControlFin();	
			
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
		
		Message msg = new Message();
		
		if(!buzon.empty()) {
			
			exitLoop = false;
			
			msg = buzon.GetFirst();
			while(!exitLoop) {
				
				if(msg.GetEstado() == 0) {
					
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
				
				finProc++;
				
				if(finProc == Isis.MAXPROCESOS) {
					
					finProc = 0;
					
					System.out.println("FIN | COMPROBACION DE LOGS");
					System.out.println();
					
					CheckLogs();
					
					semControlOrden.release();
					semControlFin.release(Isis.MAXPROCESOS - 1);
					
				} else {
					
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
	
	void Debug(String funcion, Message m) {
		
		System.out.println(" " + idProceso + " | " + funcion + " -> " + m.GetContenido());
		
	}
	
}
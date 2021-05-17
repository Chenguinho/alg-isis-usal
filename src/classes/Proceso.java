package classes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import helpers.FileLog;
import helpers.Network;
import helpers.Sleep;
import isis.Isis;

/*
 * La clase proceso contiene todos los metodos de los procesos
 * (hilos) que se ejecutaran de forma concurrente y se comunicaran
 * los unos con los otros
 */

public class Proceso extends Thread {

	//Directorio en el home del usuario en el que se crearan los ficheros log de los procesos
	private static final String FOLDER = System.getProperty("user.home") + "/isis/";
	
	//Clases de apoyo
	private Network network = new Network();
	private Sleep sleep = new Sleep();
	
	//Atributos proceso
	Integer idProceso, idEquipo;
	
	String ipServer, ipCentral;
	
	Buzon buzon;
	
	FileLog fileLog;
	
	List<String> equipos = new ArrayList<String>();
	
	//Timestamp para el tiempo logico de Lamport
	private Integer ordenProceso;
	
	//Semaforos para las secciones criticas
	private static Semaphore semControlOrden = new Semaphore(1);
	private static Semaphore semControlLineas = new Semaphore(1);
	
	//Creacion de un proceso
	public Proceso(Integer idP, Integer idE, String ip, List<String> e) {
		
		this.idProceso = idP;
		this.idEquipo = idE;
		
		this.ipServer = ip;
		
		buzon = new Buzon();
		
		fileLog = new FileLog(FOLDER, idP);
		
		this.ordenProceso = 0;
		
		this.equipos = e;
		
	}
	
	/*
	 * Metodo run() del hilo (Proceso.start())
	 * 
	 * Se avisara al servidor de que el proceso ha sido creado y se
	 * esperara a que todos los procesos se creen para continuar con
	 * la ejecucion.
	 * Crea un mensaje que se almacenara en el buzon y lo manda al servidor
	 * para que se realice el multicast, es decir, lo envie a todos los
	 * procesos.
	 * Hay que comentar que entre cada mensaje y mensaje el proceso duerme
	 * un tiempo aleatorio entre 1 y 1.5 segundos.
	 */
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
			
			for(int k = 0; k < equipos.size(); k++) {
			
				for(int j = 0; j < Isis.MAXPROCESOS; j++) {
					
					WebTarget target = network.CreateClient(equipos.get(k));
					
					target.path("multicastMsg")
						.queryParam("idMensaje", m.GetIdMensaje())
						.queryParam("idProceso", m.GetIdProceso())
						.queryParam("idDestino", j + 1)
						.queryParam("ipOrigen", this.ipServer)
						.request(MediaType.TEXT_PLAIN).get(String.class);
					
					sleep.ThreadSleep(0.2f, 0.5f);
					
				}
				
			}
				
				sleep.ThreadSleep(1.0f, 1.5f);
			
		}
		
	}
	
	/*
	 * Recepcion del primer mensaje multicast
	 * 
	 * Cuando se recibe el primer multicast del servidor se comprueba primero
	 * si se ha establecido como condicion que no se va a utilizar la multidifusion
	 * ordenada, en caso de que no, el proceso escribira en su fichero log
	 * el mensaje directamente e ira comprobando si se ha terminado con el proceso
	 * a traves del numero de lineas del fichero log.
	 * En caso de que si lo utilice, hara LC1 del orden del mensaje y comprobara
	 * de quien es el proceso para añadirlo o no al buzon (los mensajes que
	 * ha enviado el propio proceso ya se encuentran en su buzon y tendra que
	 * encargarse de almacenar el nuevo orden obtenido).
	 * Una vez realizadas todas las acciones enviara la propuesta al proceso que ha
	 * enviado el multicast.
	 */
	public void receiveMulticast(Integer idM, Integer idP, Integer idOrigen, Integer ordered, String ipDestino) {

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
				
				SendPropuesta(idM, idP, ordenProceso, idOrigen, idProceso, ipDestino);
				
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
		
		
	
	/*
	 * Recepcion de la propuesta del mensaje multicast
	 * 
	 * Cuando se recibe la propuesta del mensaje se hace el LC2 del orden del mensaje
	 * y del timestamp del proceso. Se modificara el orden del mensaje almacenado en
	 * el buzon en caso de que sea necesario y se aumentara el numero de propuestas
	 * recibidas para ese mensaje.
	 * Si las propuestas recibidas son tantas como procesos se han creado inicialmente
	 * se realizara la difusion del acuerdo.
	 */
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
	
	/*
	 * Recepcion del acuerdo
	 * 
	 * Al recibir el acuerdo se hara LC2 del timestamp del proceso y del orden del mensaje
	 * recibido, se modificaran los atributos necesarios para tener en el buzon la informacion
	 * correcta, se ordenara el buzon y se hara la escritura en el fichero log del proceso.
	 * Una vez realizada la escritura se comprueba si se ha terminado con todos los mensajes
	 * obteniendo el numero de lineas del fichero log, si se ha terminado se avisara al servidor
	 * para que haga las comprobaciones de los logs.
	 */
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
	
	//Metodo para controlar la finalizacion del proceso (multidifusion ordenada)
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
	
	//Metodo para controlar la finalizacion del proceso (multidifusion no ordenada)
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
		
		for(int i = 0; i < equipos.size(); i++) {
			
			WebTarget target = network.CreateClient(equipos.get(i));
			
			target.path("waitForProcs")
			.request(MediaType.TEXT_PLAIN).get(String.class);
		}
		
	}
	
	//Enviar la propuesta al proceso
	void SendPropuesta(Integer idM, Integer idP, Integer orden, Integer idDestino, Integer idDesempate, String ip) {
		
		WebTarget target = network.CreateClient(ip);
		
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
		
		for(int i = 0; i < equipos.size(); i++) {
		
			for(int j = 0; j < Isis.MAXPROCESOS; j++) {
				
				WebTarget target = network.CreateClient(equipos.get(i));
				
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
		
	}
	
	//Llamada a servidor para comprobacion de logs (multidifusion ordenada)
	void CheckLogs() {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("checkLogs")
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Llamada a servidor para comprobacion de logs (multidifusion no ordenada)
	void CheckLogs2() {
		
		WebTarget target = network.CreateClient(ipServer);
		
		target.path("checkLogs2")
			.request(MediaType.TEXT_PLAIN).get(String.class);
		
	}
	
	//Metodos para controlar los tiempos logicos
	
	/*
	 * Tiempo logico de Lamport (1)
	 * Incrementa en 1 el orden del mensaje
	 */
	public Integer LC1(Integer orden) {
		
		return orden + 1;
		
	}
	
	/*
	 * Tiempo logico de Lamport (2)
	 * Recibe el timestamp del proceso y lo compara con
	 * el contador interno del mensaje.
	 * 	- Si es mayor el timestamp del proceso lo incrementara
	 * 		en uno y lo guardara como su orden
	 * 	- Si es mayor el contador del mensaje lo incrementara
	 * 		en uno y lo guardara como su orden 
	 */
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
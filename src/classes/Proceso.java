package classes;

import java.time.LocalDateTime;
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
	
	//Flag para abandonar el bucle de escritura
	
	boolean exitLoop = false;
	
	//Semaforos
	
	public static Semaphore semControlOrder = new Semaphore(1);
	static Semaphore semControlBuzon = new Semaphore(1);
	
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
		
		String tStart = String.format("%02d", LocalDateTime.now().getHour())
						+ ":" +
						String.format("%02d", LocalDateTime.now().getMinute())
						+ ":" +
						String.format("%02d", LocalDateTime.now().getSecond());
		
		fileLog.log(fileLog.GetFileName(), "Proceso " + idProceso + " iniciado a las " + tStart);
		
		for(int i = 0; i < 10; i++) {
			
			Message m = new Message(i + 1, idEquipo, idProceso, 0, 0, 0);
			
			buzon.AddMessage(m);
			
			for(int j = 0; j < Isis.MAXPROCESOS; j++) {
				
				System.out.println("MULT | " + idProceso + " Envio " + m.GetContenido() + " a " + (j+1));
				
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
		
		try{
			
			semControlOrder.acquire();
			
			m.SetOrdenLC1();
			ordenProceso = m.GetOrden();
			
			if(m.GetIdProceso() != idProceso)
				buzon.AddMessage(m);
		
			semControlOrder.release();
			
			System.out.println("PROP | " + idProceso + " Envio " + m.GetContenido() + " a " + idOrigen);
			
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
			
			Message mensajeBuzon = buzon.GetMessage(m);
			
			buzon.DeleteMessage(mensajeBuzon);
			
			if(mensajeBuzon.GetOrden() < m.GetOrden())
				mensajeBuzon.SetOrden(m.GetOrden());
			
			mensajeBuzon.SetPropuestas(mensajeBuzon.GetPropuestas() + 1);
			
			if(mensajeBuzon.GetPropuestas() == Isis.MAXPROCESOS) {
				
				mensajeBuzon.SetEstado(1);
				
				buzon.AddMessage(mensajeBuzon);
				
				semControlOrder.release();
				
				SendAcuerdo(mensajeBuzon);
				
			} else {
				
				buzon.AddMessage(mensajeBuzon);
				
				semControlOrder.release();
				
			}
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//Recepcion del acuerdo
	
	public void receiveAcuerdo(Message m) {
		
		try {
			
			semControlOrder.acquire();
			
			//Tiempo logico de Lamport
			
			m.SetOrdenLC2(ordenProceso);
			ordenProceso = m.GetOrden();
			
			//Obtengo el mensaje del buzon con el id correspondiente
			
			Message mensajeBuzon = buzon.GetMessage(m);
			
			//Lo elimino del buzon
			
			buzon.DeleteMessage(mensajeBuzon);
			
			//Se hacen las comprobaciones y modificaciones necesarias
			
			if(mensajeBuzon.GetOrden() < m.GetOrden())
				mensajeBuzon.SetOrden(m.GetOrden());
			
			mensajeBuzon.SetEstado(1);
			
			//Se vuelve a almacenar en el buzon
			
			buzon.AddMessage(mensajeBuzon);
			
			//Ordenamos el buzon (ORDEN DESC)
			
			if(buzon.GetBuzonLength() >= 2)
				buzon.Order();
			
			//Escribir en el log (equivalente a entrega)
			
			ImprimirBuzon();
			
			semControlOrder.release();	
			
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
				" |");
			
		}
		System.out.println();
		
	}
	
	//Metodos GET
	
	public Integer GetIdProceso() {
		
		return this.idProceso;
		
	}
	
}
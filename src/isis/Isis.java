package isis;

import classes.FileLog;
import classes.Message;
import classes.Proceso;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

@Path("isis")
public class Isis {
	
	public static final int MAXCOMPS = 1;
	public static final int MAXPROCESOS = 2 * MAXCOMPS;
	
	private static final String LOGFOLDER = System.getProperty("user.home") + "/isis/";
	private static final String LOGSEND = "LogSend.txt";
	private static final String LOGMAIL = "LogMail.txt";
	
	private static int cont;
	
	private static Semaphore semControlCont = new Semaphore(1);
	private static Semaphore semControlProc = new Semaphore(0);
	
	public static Semaphore semControlMulti = new Semaphore(1);
	
	private List<String> equipos = new ArrayList<String>();
	private static Map<Integer, String> mapaProcesos = new HashMap<Integer, String>();
	
	public static List<Proceso> listaProcesos;
	
	public String ipServer;

	@GET
	@Path("hola")
	public void hola() {
		System.out.println("Hola buenas");
	}
	
	@GET
	@Path("start")
	public void start() {
		
		listaProcesos = new ArrayList<Proceso>();
		
		/*
		 * Pedimos al usuario a traves de la consola que introduzca manualmente
		 * tantas IPs como ordenadores establezcamos como maximo
		 */
		
		Scanner sc = new Scanner(System.in);
		for(int i = 0; i < MAXCOMPS; i++) {
			System.out.printf("Introduce la IP del ordenador %d\n", i + 1);
			String input = sc.next();
			equipos.add(i, input);
			if(i == 0) {
				ipServer = input;
			}
		}
		sc.close();
		
		/*
		 * Construimos una URL a traves de la cual crearemos los servicios en
		 * cada una de las IPs, esta URL tendrá la estructura
		 * http://[IP de la máquina]:8080/practicaFinal/isis/create
		 * Como parámetros enviaremos el numero de ordenador que corresponda
		 */
		
		for(int i = 0; i < MAXCOMPS; i++) {
			Client client = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + equipos.get(i) + ":8080/practicaFinal/isis").build();
			WebTarget target = client.target(uri);
			
			target.path("create")
			.queryParam("computer", String.valueOf(i + 1))
			.queryParam("dir", equipos.get(i))
			.request(MediaType.TEXT_PLAIN)
			.get(String.class);
		}
	}
	
	@GET
	@Path("create")
	public void crear(
			@DefaultValue("0")
			@QueryParam(value="computer") Integer computer,
			@DefaultValue("localhost")
			@QueryParam(value="dir") String ip
	) {
		
		int idProc1 = computer + (computer - 1);
		int idProc2 = idProc1 + 1;

		FileLog logger1 = new FileLog(LOGFOLDER, LOGFOLDER + idProc1 + LOGSEND, LOGFOLDER + idProc1 + LOGMAIL);
		FileLog logger2 = new FileLog(LOGFOLDER, LOGFOLDER + idProc2 + LOGSEND, LOGFOLDER + idProc2 + LOGMAIL);
		
		Proceso proc1 = new Proceso(computer + (computer - 1), computer, equipos, ip, logger1, ipServer);
		Proceso proc2 = new Proceso(computer + computer, computer, equipos, ip, logger2, ipServer);
		
		listaProcesos.add(proc1);
		proc1.start();
		
		listaProcesos.add(proc2);
		proc2.start();
		
		/*
		 * Para depués saber a dónde enviamos la información y no tener que hacer 
		 * sucesiones de if creamos un mapa en el que asociamos a la clave (ID del
		 * proceso) su dirección IP y poder accederlas directamente
		 */
		
		mapaProcesos.put(computer + (computer - 1), ip);
		mapaProcesos.put(computer + computer, ip);
		
	}
	
	/*
	 * Función para esperar a que todos los procesos estén listos y no
	 * empezar con alguno pendiente de inicializarse
	 */
	
	@GET
	@Path("waitForProcs")
	public void waitForProcs() {
		
		try {
			
			semControlCont.acquire();
			
			cont++;
			
			if(cont == MAXPROCESOS) {
				System.out.println("Todos listos, empieza");
				System.out.println("_____________________");
				System.out.println();
				cont = 0;
				semControlCont.release();
				semControlProc.release(MAXPROCESOS - 1);
			} else {
				System.out.println("Esperando a todos los procesos...");
				semControlCont.release();
				semControlProc.acquire();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 * Proceso para enviar el primer mensaje a todos los procesos
	 */
	
	@GET
	@Path("multicastMsg")
	public void multicastMsg(
			@QueryParam(value="content") String content,
			@QueryParam(value="id") Integer id,
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="order") Long order,
			@QueryParam(value="ipServer") String ipServer,
			@QueryParam(value="idDest") Integer idDest
	) {
		
		Message m = new Message(id, idEquipo, idProceso, order, 0);
		
		System.out.println("MULTICAST | " + m.GetContent() + " enviado a proceso " + idDest);
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			if(listaProcesos.get(i).GetIdProceso() == idDest)
				listaProcesos.get(i).receiveMulticast(m, m.GetProcess());
			
		}
		
	}
	
	@GET
	@Path("sendPurpose")
	public void multicastPurpose(
			@QueryParam(value="content") String content,
			@QueryParam(value="id") Integer id,
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="idProcMen") Integer idProcMen,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="order") Long order,
			@QueryParam(value="ipServer") String ipServer,
			@QueryParam(value="idDest") Integer idDest,
			@QueryParam(value="propuestas") Integer propuestas
	) {
		
		Message m = new Message(id, idEquipo, idProcMen, order, 0);
		
		System.out.println("PROPUESTA | " + m.GetContent() + " de proceso " + idProceso + " a proceso " + idDest);
			
		listaProcesos.get(idDest - 1).receivePurpose(m);
		
	}
	
	@GET
	@Path("sendMultiDef")
	public void multicastDef(
			@QueryParam(value="content") String content,
			@QueryParam(value="id") Integer id,
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="order") Long order,
			@QueryParam(value="ipServer") String ipServer,
			@QueryParam(value="idDest") Integer idDest,
			@QueryParam(value="propuestas") Integer propuestas
	) {
		
		
		
	}
	
}
package isis;

import classes.Message;
import classes.Proceso;
import helpers.Commands;
import helpers.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

@Path("isis")
public class Isis {
	
	static final int MAXCOMPS = 1;
	public static final int MAXPROCESOS = 2 * MAXCOMPS;
	public static final int NUMMENSAJES = 100;
	
	private Network network = new Network();
	private Commands com = new Commands();
	
	//private String ipServidorCentral;
	
	private static List<String> listaEquipos;
	public static List<Proceso> listaProcesos;
	
	private static int contadorProcesos;
	private static Semaphore semControlContador = new Semaphore(1);
	private static Semaphore semControlProcesos = new Semaphore(0);
	
	//public String ipServer;
	
	@GET
	@Path("start")
	public void start() {
		
		listaProcesos = new ArrayList<Proceso>();
		listaEquipos = new ArrayList<String>();
		
		/*
		 * Pedimos al usuario a través de consola
		 * que introduzca la dirección IP de tantos
		 * ordenadores como establezcamos en MAXCOMPS
		 */
		
		Scanner sc = new Scanner(System.in);
		
		for(int i = 0; i < MAXCOMPS; i++) {
			
			System.out.println("Introduce la IP del equipo " + (i + 1));
			
			String input = sc.next();
			
			//if(i == 0)
			//	ipServidorCentral = input;
			
			listaEquipos.add(input);
			
		}
		
		sc.close();
		
		/*
		 * Empezamos a crear los procesos en las diferentes
		 * direcciones que hemos recibido como parametros
		 * a traves de la consola
		 */
		
		for(int i = 0; i < MAXCOMPS; i++) {
			
			WebTarget target = network.CreateClient(listaEquipos.get(i));
			
			target.path("create")
				.queryParam("idEquipo", i + 1)
				.queryParam("ipEquipo", listaEquipos.get(i))
				.request(MediaType.TEXT_PLAIN).get(String.class);
			
		}
		
	}
	
	//Funcion para crear los procesos
	
	@GET
	@Path("create")
	public void create(
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="ipEquipo") String ipEquipo
	) {
		
		Integer idP1 = idEquipo + (idEquipo - 1);
		Integer idP2 = idP1 + 1;
		
		Proceso p1 = new Proceso(idP1, idEquipo, ipEquipo);
		Proceso p2 = new Proceso(idP2, idEquipo, ipEquipo);
		
		listaProcesos.add(p1);
		p1.start();
		
		listaProcesos.add(p2);
		p2.start();
		
	}
	
	//Funcion para esperar y sincronizar los procesos
	
	@GET
	@Path("waitForProcs")
	public void waitProcs() {
		
		try {
			
			semControlContador.acquire();
			
			contadorProcesos++;
			
			if(contadorProcesos != MAXPROCESOS) {
				
				System.out.println("Esperando a todos los procesos...");
				
				semControlContador.release();
				semControlProcesos.acquire();
				
			} else {
				
				System.out.println("Todos los procesos listos!");
				System.out.println("EMPIEZA...");
				System.out.println();
				
				contadorProcesos = 0;
				
				semControlContador.release();
				semControlProcesos.release(MAXPROCESOS - 1);
				
			}
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	@GET
	@Path("multicastMsg")
	public void multicastMsg(
			@QueryParam(value="idMensaje") Integer idMensaje,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="idDestino") Integer idDestino
	) {
		
		Message m = new Message(idMensaje, idEquipo, idProceso, 0, 0, 0);
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			if(listaProcesos.get(i).GetIdProceso() == idDestino)
				listaProcesos.get(i).receiveMulticast(m, idProceso);
			
		}
		
	}
	
	@GET
	@Path("sendPropuesta")
	public void sendPropuesta(
			@QueryParam(value="idMensaje") Integer idMensaje,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="orden") Integer orden,
			@QueryParam(value="idDestino") Integer idDestino
	) {
		
		Message m = new Message(idMensaje, idEquipo, idProceso, orden, 0, 0);
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			if(listaProcesos.get(i).GetIdProceso() == idDestino)
				listaProcesos.get(i).receivePropuesta(m);	
		}
		
		
		
	}
	
	@GET
	@Path("sendAcuerdo")
	public void sendAcuerdo(
			@QueryParam(value="idMensaje") Integer idMensaje,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="orden") Integer orden,
			@QueryParam(value="numPropuestas") Integer numPropuestas
	) {
		
		Message m = new Message(idMensaje, idEquipo, idProceso, orden, numPropuestas, 1);
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			listaProcesos.get(i).receiveAcuerdo(m);
			
		}
		
	}
	
	@GET
	@Path("checkLogs")
	public void checkLogs() {
		
		com.Exec();
		
	}
	
}
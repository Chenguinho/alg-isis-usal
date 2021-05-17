package isis;

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

/*
 * La clase isis representa el servidor, es decir, los metodos
 * que se van a ejecutar en el servidor accediendo a ellos
 * a traves de rutas conocidas.
 * Contendra toda la informacion necesaria para poder comunicarse
 * con todos los procesos
 */

@Path("isis")
public class Isis {
	
	//Constantes
	static final int MAXCOMPS = 3;
	static final int NPROC = 2;
	public static final int MAXPROCESOS = NPROC * MAXCOMPS;
	public static final int NUMMENSAJES = 100;
	
	//Clases de apoyo
	private Network network = new Network();
	private Commands com = new Commands();
	
	//Listas para almacenar informacion relevante
	private static List<String> listaEquipos = new ArrayList<String>();
	public static List<Proceso> listaProcesos;
	
	//Semaforos para control de secciones criticas
	private static Semaphore semControlContador = new Semaphore(1);
	private static Semaphore semControlProcesos = new Semaphore(0);
	
	//Contadores para controlar la creacion y finalizacion de procesos
	private static Integer contadorProcesos = 0;
	private static Integer procesosFinalizados = 0;
	
	//Variable para activar o no la multidifusion ordenada
	private static Integer ordered;
	
	/*
	 * Inicializacion de los procesos, se pide por consola que se introduzcan
	 * las direcciones IP de los equipos en los que crearemos dos procesos, tantos
	 * como establezcamos en MAXCOMPS.
	 */
	@GET
	@Path("start")
	public void start() {
		
		Scanner sc = new Scanner(System.in);
		
		do {
			System.out.println("¿Va a desear emplear el protocolo de multidifusión ordenada?");
			System.out.println("(Introduzca 1 -> SI o 0 -> NO)");
			ordered = sc.nextInt();
		} while(ordered != 1 && ordered != 0);
		
		for(int i = 0; i < MAXCOMPS; i++) {
			
			System.out.println("Introduce la IP del equipo " + (i + 1));
			
			String input = sc.next();
			
			listaEquipos.add(input);
			
		}
		
		sc.close();
		
		for(int i = 0; i < MAXCOMPS; i++) {
			
			WebTarget target = network.CreateClient(listaEquipos.get(i));
			
			target.path("create")
				.queryParam("idEquipo", i + 1)
				.queryParam("ipEquipo", listaEquipos.get(i))
				.queryParam("equipos", listaEquipos)
				.queryParam("ordered", ordered)
				.request(MediaType.TEXT_PLAIN).get(String.class);
			
		}
		
	}
	
	/*
	 * Creamos dos procesos y los iniciamos en cada una de las direcciones 
	 * IP que hemos obtenido en la ruta start
	 */
	@GET
	@Path("create")
	public void create(
			@QueryParam(value="idEquipo") Integer idEquipo,
			@QueryParam(value="ipEquipo") String ipEquipo,
			@QueryParam(value="equipos") List<String> equipos,
			@QueryParam(value="ordered") Integer isOrdered
	) {
		
		listaProcesos = new ArrayList<Proceso>();
		String[] ips = new String[MAXCOMPS];
		List<String> computerList = new ArrayList<String>();
		ordered = isOrdered;		
		
		for(int k = 0; k < equipos.size(); k++) {
			
			String e = equipos.get(k).substring(1, equipos.get(k).length() - 1);
			ips = (e.split(","));
		}
		
		for(int i = 0; i < ips.length; i++) {
			
			computerList.add(ips[i].trim());
			
		}
		
		Integer idP1 = idEquipo + (idEquipo - 1);
		Integer idP2 = idP1 + 1;
		
		Proceso p1 = new Proceso(idP1, idEquipo, ipEquipo, computerList);
		Proceso p2 = new Proceso(idP2, idEquipo, ipEquipo, computerList);
		
		listaProcesos.add(p1);
		p1.start();
		
		listaProcesos.add(p2);
		p2.start();
		
	}
	
	/*
	 * Funcion para esperar a que se creen todos los procesos y que estos
	 * esten sincronizados
	 */
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
	
	/*
	 * MULTICAST MENSAJE
	 * 
	 * El servidor recibe desde el proceso la informacion del mensaje que
	 * tendra que mandarle a todos los procesos (se recibe en idDestino
	 * el ID del proceso al que tiene que mandarselo).
	 * Tambien envia la informacion relacionada a la multidifusion
	 * ordenada, es decir, si se tiene que utilizar el algoritmo o no.
	 */
	@GET
	@Path("multicastMsg")
	public void multicastMsg(
			@QueryParam(value="idMensaje") Integer idMensaje,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="idDestino") Integer idDestino,
			@QueryParam(value="ipOrigen") String ipOrigen
	) {
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			if(listaProcesos.get(i).GetIdProceso() == idDestino)
				listaProcesos.get(i).receiveMulticast(idMensaje, idProceso, idProceso, ordered, ipOrigen);
			
		}
		
	}
	
	/*
	 * PROPUESTA
	 * 
	 * El servidor recibe la informacion de la propuesta que tiene que enviarle
	 * al proceso que se indica en idDestino
	 */
	@GET
	@Path("sendPropuesta")
	public void sendPropuesta(
			@QueryParam(value="idMensaje") Integer idMensaje,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="orden") Integer orden,
			@QueryParam(value="idDestino") Integer idDestino,
			@QueryParam(value="idDesempate") Integer idDesempate
	) {
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			if(listaProcesos.get(i).GetIdProceso() == idDestino)
				listaProcesos.get(i).receivePropuesta(idMensaje, idProceso, orden, idDesempate);	
			
		}
		
		
		
	}
	
	/*
	 * MULTICAST ACUERDO
	 * 
	 * El servidor recibe desde el proceso la informacion del acuerdo que
	 * tendra que mandarle a todos los procesos (se recibe en idDestino
	 * el ID del proceso al que tiene que mandarselo), tambien se envia
	 * el ID del proceso que envio la propuesta para que en caso de que
	 * se necesite desempatar el orden del buzon se haga segun las condiciones
	 * establecidas.
	 */
	@GET
	@Path("sendAcuerdo")
	public void sendAcuerdo(
			@QueryParam(value="idMensaje") Integer idMensaje,
			@QueryParam(value="idProceso") Integer idProceso,
			@QueryParam(value="orden") Integer orden,
			@QueryParam(value="numPropuestas") Integer numPropuestas,
			@QueryParam(value="idDesempate") Integer idDesempate,
			@QueryParam(value="destino") Integer destino
	) {
		
		for(int i = 0; i < listaProcesos.size(); i++) {
			
			if(listaProcesos.get(i).GetIdProceso() == destino)
				listaProcesos.get(i).receiveAcuerdo(idMensaje, idProceso, orden, numPropuestas, idDesempate);
			
		}
		
	}
	
	/*
	 * COMPROBACION LOGS MULTIDIFUSION ORDENADA
	 * 
	 * Se comprueba si los ficheros log de todos los procesos son iguales
	 */
	@GET
	@Path("checkLogs")
	public void checkLogs() {
		
		procesosFinalizados++;
		System.out.println("ding-dong!");
		
		if(procesosFinalizados == NPROC) {
			System.out.println("Los " + procesosFinalizados + " han llegado!");
			System.out.println("A ver si han llegado enteros...\n");
			
			com.Exec();
		}
		
	}
	
	/*
	 * COMPROBACION LOGS MULTIDIFUSION ORDENADA
	 * 
	 * Se comprueba si los ficheros log de todos los procesos son iguales
	 */
	@GET
	@Path("checkLogs2")
	public void checkLogs2() {
		
		procesosFinalizados++;
		System.out.println("pum-pum-pum!");
		
		if(procesosFinalizados == NPROC) {
			System.out.println("Los " + procesosFinalizados + " han llegado!");
			System.out.println("Estos tienen que ser unos desgraciados...\n");
			
			com.Exec();
		}
		
	}
	
}
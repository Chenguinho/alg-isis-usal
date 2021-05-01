package isis;

import classes.FileLog;
import classes.Proceso;
import classes.Semaforo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
	
	private static final int MAXCOMPS = 1;
	private static final int MAXPROCESOS = 2 * MAXCOMPS;
	
	private static final String LOGFOLDER = System.getProperty("user.home") + "/isis/";
	private static final String LOGSEND = "LogSend.txt";
	private static final String LOGMAIL = "LogMail.txt";
	
	private static int cont;
	
	private Semaphore semControlCont = new Semaphore(1);
	private Semaphore semControlProc = new Semaphore(0);
	
	
	private List<String> equipos = new ArrayList<String>();

	@GET
	@Path("hola")
	public void hola() {
		System.out.println("Hola buenas");
	}
	
	@GET
	@Path("start")
	public void start() {
		
		/*
		 * Pedimos al usuario a traves de la consola que introduzca manualmente
		 * tantas IPs como ordenadores establezcamos como maximo
		 */
		
		Scanner sc = new Scanner(System.in);
		for(int i = 0; i < MAXCOMPS; i++) {
			System.out.printf("Introduce la IP del ordenador %d\n", i + 1);
			equipos.add(i, sc.next());
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
		
		List<Proceso> listaProcesos = new ArrayList<Proceso>();
		
		int idProc1 = computer + (computer - 1);
		int idProc2 = idProc1 + 1;

		FileLog logger1 = new FileLog(LOGFOLDER, LOGFOLDER + idProc1 + LOGSEND, LOGFOLDER + idProc1 + LOGMAIL);
		FileLog logger2 = new FileLog(LOGFOLDER, LOGFOLDER + idProc2 + LOGSEND, LOGFOLDER + idProc2 + LOGMAIL);
		
		Proceso proc1 = new Proceso(computer + (computer - 1), computer, equipos, ip, logger1);
		Proceso proc2 = new Proceso(computer + computer, computer, equipos, ip, logger2);
		
		listaProcesos.add(proc1);
		listaProcesos.get(0).start();
		
		listaProcesos.add(proc2);
		listaProcesos.get(1).start();
		
	}
	
	@GET
	@Path("waitForProcs")
	public void waitForProcs() {
		
		try {
			
			semControlCont.acquire();
			
			cont++;
			
			if(cont == MAXPROCESOS) {
				cont = 0;
				semControlCont.release();
				semControlProc.release(MAXPROCESOS - 1);
			} else {
				semControlCont.release();
				semControlProc.acquire();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	
}
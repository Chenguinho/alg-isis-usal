package isis;

import classes.Proceso;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
	private static final int MAXPROCESOS = 2;
	
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
		
		for(int i = computer; i < computer + 2; i++) {
			Proceso proc = new Proceso((computer * 2) + 1, computer, equipos);
			listaProcesos.add(proc);
		}
		
	}
	
}
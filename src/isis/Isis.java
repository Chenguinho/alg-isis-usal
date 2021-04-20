package isis;

import java.net.URI;
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
	
	private String[] computerIps = new String[MAXCOMPS];

	@GET
	@Path("hola")
	public void hola() {
		System.out.println("Hola buenas");
	}
	
	@GET
	@Path("start")
	public void start() {
		Scanner sc = new Scanner(System.in);
		for(int i = 0; i < MAXCOMPS; i++) {
			System.out.printf("Introduce la IP del ordenador %d\n", i + 1);
			computerIps[i] = sc.next();
		}
		
		for(int i = 0; i < MAXCOMPS;i++) {
			Client client = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + computerIps[i] + ":8080/practicaFinal/isis").build();
			WebTarget target = client.target(uri);
			
			target.path("create")
			.queryParam("computer", String.valueOf(i + 1))
			.queryParam("dir", computerIps[i])
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
		
		System.out.println("Hola " + computer + " " + ip);
		
	}
	
}

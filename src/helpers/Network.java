package helpers;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

public class Network {

	public WebTarget CreateClient(String ip) {
		
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://" + ip + ":8080/practicaFinal/isis").build();
		WebTarget target = client.target(uri);
		
		return target;
		
	}
	
}

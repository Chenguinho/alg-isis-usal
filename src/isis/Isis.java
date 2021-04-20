package isis;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("isis")
public class Isis {

	@GET
	@Path("crear")
	public void crear() {
		Multi m = new Multi();
		m.start();
		
		MailTail t = new MailTail();
		t.start();
	}
	
}

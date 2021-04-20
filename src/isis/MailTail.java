package isis;

import java.util.ArrayList;
import java.util.Date;

import classes.*;

public class MailTail extends Thread {

	public void run() {
		
		Date date = new Date();
		
		Mail m = new Mail(new ArrayList<Message>(), date.getTime());
	}
	
}

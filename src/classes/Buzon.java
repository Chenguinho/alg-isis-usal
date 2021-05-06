package classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Buzon {

	List<Message> buzon;
	
	public Buzon() {
		
		this.buzon = new ArrayList<Message>();
		
	}
	
	public Message GetMessage(Message m) {
		
		Message returnMessage = new Message();
		
		for(int i = 0; i < buzon.size(); i++) {
			
			if(buzon.get(i).GetIdMensaje() == m.GetIdMensaje() && buzon.get(i).GetIdProceso() == m.GetIdProceso())
				returnMessage = buzon.get(i);
			
		}
		
		return returnMessage;
		
	}
	
	public void DeleteMessage(Message m) {
		
		for(int i = 0; i < buzon.size(); i++) {
			
			if(buzon.get(i).GetIdMensaje() == m.GetIdMensaje() && buzon.get(i).GetIdProceso() == m.GetIdProceso())
				buzon.remove(i);
			
		}
		
	}
	
	public void DeleteMessage() {
		
		buzon.remove(0);
		
	}
	
	public void AddMessage(Message m) {
		
		buzon.add(m);
		
	}
	
	public void Order() {
		
		Collections.sort(buzon, new Comparator<Message>() {
			public int compare(Message m1, Message m2) {
				if(m1.GetOrden() > m2.GetOrden()) return -1;
				if(m1.GetOrden() < m2.GetOrden()) return 1;
				return 0;
			}
		}
		);
		
	}
	
	public int GetBuzonLength() {
		
		return this.buzon.size();
		
	}
	
	public boolean empty() {
		
		if(buzon.isEmpty())
			return true;
		else
			return false;
		
	}
	
	public Message GetMessage() {
		
		return buzon.get(0);
		
	}
	
	public List<Message> GetBuzonList(){
		
		return this.buzon;
		
	}
	
}

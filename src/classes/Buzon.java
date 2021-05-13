package classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Buzon {

	List<Message> buzon;
	
	int compare;
	
	public Buzon() {
		
		this.buzon = new ArrayList<Message>();
		
	}
	
	public Message GetMessage(Integer idM, Integer idP) {
		
		Message returnMessage = new Message();
		
		for(int i = 0; i < buzon.size(); i++) {
			
			if(buzon.get(i).GetIdMensaje() == idM && buzon.get(i).GetIdProceso() == idP)
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
	
	public void RemoveFirst() {
		
		buzon.remove(0);
		
	}
	
	public void AddMessage(Message m) {
		
		buzon.add(m);
		
	}
	
	public void Order(Integer desempate) {
		
		Collections.sort(buzon, new Comparator<Message>() {
			
			public int compare(Message m1, Message m2) {
				
				if(m2.GetOrden().compareTo(m1.GetOrden()) != 0) {
					return m1.GetOrden().compareTo(m2.GetOrden());
				} else {
					if(m1.GetIdProceso().compareTo(desempate) == 0) return 1;
					else return -1;
				}
				
			}
			
		});
		
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
	
	public Message GetFirst() {
		
		return buzon.get(0);
		
	}
	
	public List<Message> GetBuzonList(){
		
		return this.buzon;
		
	}
	
}

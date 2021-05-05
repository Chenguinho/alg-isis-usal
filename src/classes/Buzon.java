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
	
	public Message GetMessage(int idMessage, int idProceso) {
		
		for(int i = 0; i < buzon.size(); i++) {
			
			if(idMessage == buzon.get(i).GetIdMensaje() 
					&& 
					idProceso == buzon.get(i).GetIdProceso())
				return buzon.get(i);
		}
		
		return null;
		
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
	
	public List<Message> GetBuzonList(){
		
		return this.buzon;
		
	}
	
}

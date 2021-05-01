package classes;

import java.util.ArrayList;
import java.util.List;

public class Mail {
	
	List<Message> buzon = new ArrayList<Message>();
	long clock = 0;
	
	//Constructor
	public Mail(List<Message> l, long c) {
		this.buzon = l;
		this.clock = c;
	}
	
	public Mail() {
		
	}
	
	//Métodos constructor
	
	//SET -> Guardar valores
	
	public void SetClock(long c) {
		this.clock = c;
	}
	
	public void SetBuzon(List<Message> l) {
		this.buzon = l;
	}
	
	//GET -> Obtener valores
	
	public long GetClock() {
		return this.clock;
	}
	
	public List<Message> GetBuzon(){
		return this.buzon;
	}

}

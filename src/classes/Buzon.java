package classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * La clase buzon es donde se iran almacenando los mensajes
 * cuando los procesos lo reciban, es decir, tiene una
 * lista de objetos de tipo Message y los metodos necesarios
 * para actuar sobre estos
 */

public class Buzon {

	List<Message> buzon;
	
	//Creacion del buzon
	public Buzon() {
		
		this.buzon = new ArrayList<Message>();
		
	}
	
	//Metodo para añadir a la lista un mensaje pasado como parametro
	public void AddMessage(Message m) {
		
		buzon.add(m);
		
	}
	
	//Metodo praa obtener el primer mensaje de la lista
	public Message GetFirst() {
		
		return buzon.get(0);
		
	}
	
	//Metodo para eliminar el primer mensaje del buzon
	public void RemoveFirst() {
		
		buzon.remove(0);
		
	}
	
	/*
	 * Metodo para obtener un mensaje en concreto del buzon
	 * segun su ID (formado por el ID del mensaje y el ID del
	 * proceso)
	 */
	public Message GetMessage(Integer idM, Integer idP) {
		
		Message returnMessage = new Message();
		
		for(int i = 0; i < buzon.size(); i++) {
			
			if(buzon.get(i).GetIdMensaje() == idM && buzon.get(i).GetIdProceso() == idP)
				returnMessage = buzon.get(i);
			
		}
		
		return returnMessage;
		
	}
	
	/*
	 * Metodo para borrar un mensaje concreto del buzon, pasando
	 * este como parametro.
	 * Buscara en la lista el elemento con el mismo ID tanto de mensaje
	 * como de proceso y lo eliminara
	 */
	public void DeleteMessage(Message m) {
		
		for(int i = 0; i < buzon.size(); i++) {
			
			if(buzon.get(i).GetIdMensaje() == m.GetIdMensaje() && buzon.get(i).GetIdProceso() == m.GetIdProceso())
				buzon.remove(i);
			
		}
		
	}
	
	/*
	 * Metodo que ordena el buzon segun el orden del mensaje, en caso
	 * de empate se utiliza para desempatar el id del proceso que
	 * envio la propuesta
	 */
	public void Order(final Integer var) {
		
		Collections.sort(buzon, new Comparator<Message>() {
			
			public int compare(Message m1, Message m2) {
				
				if(m2.GetOrden().compareTo(m1.GetOrden()) != 0) {
					return m1.GetOrden().compareTo(m2.GetOrden());
				} else {
					if(m1.GetIdProceso().compareTo(var) == 0) return 1;
					else return -1;
				}
				
			}
			
		});
		
	}
	
	//Metodo para obtener la longitud de la lista de mensajes
	public int GetBuzonLength() {
		
		return this.buzon.size();
		
	}
	
	//Metodo para saber si la lista esta o no vacia
	public boolean empty() {
		
		if(buzon.isEmpty())
			return true;
		else
			return false;
		
	}
	
	//Metodos GET
	
	public List<Message> GetBuzonList(){
		
		return this.buzon;
		
	}
	
}

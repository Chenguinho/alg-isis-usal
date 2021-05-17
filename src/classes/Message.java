package classes;

/*
 * La clase mensaje representa el objeto que se mandaran
 * los procesos y que contiene toda la informacion necesaria
 * para que estos se comuniquen y hagan las funciones de 
 * forma correcta
 */

public class Message {
	
	//Constantes que representan el estado del mensaje
	static final String PROV = "PROVISIONAL";
	static final String DEF = "DEFINITIVO";
	
	//Atributos del mensaje
	Integer idMensaje, idEquipo, idProceso;
	Integer orden;
	Integer numPropuestas;
	
	String estado;
	
	String contenido;
	
	//Creacion de un mensaje
	public Message() {
		
	}
	
	//Creacion de un mensaje a partir de atributos dados como parametros
	public Message(Integer idM, Integer idP, Integer orden, Integer prop, int estado) {
		
		this.idMensaje = idM;
		this.idProceso = idP;
		
		this.numPropuestas = prop;
		this.orden = orden;
		
		SetEstado(estado);
		
		SetContenido(idP, idM);
		
	}
	
	//Creacion de un mensaje a partir de otro
	public Message(Message m) {
		
		this.idMensaje = m.GetIdMensaje();
		this.idEquipo = m.GetIdEquipo();
		this.idProceso = m.GetIdProceso();
		
		this.numPropuestas = m.GetPropuestas();
		this.orden = m.GetOrden();
		
		SetEstado(m.GetEstado());
		
		SetContenido(m.GetIdProceso(), m.GetIdMensaje());
		
	}
	
	//Metodos SET
	
	/*
	 * Establece el estado de un mensaje a partir de un entero:
	 * 		0 => Provisional
	 * 		1 => Definitivo
	 */
	public void SetEstado(int i) {
		
		if(i == 0)
			this.estado = PROV;
		else
			this.estado = DEF;
		
	}
	
	/*
	 * Establece el contenido de un mensaje en el formato
	 * necesario:
	 * PXX MMM
	 * 		XX 	=> Identificador del proceso
	 * 		MMM => Identificador del mensaje
	 */
	public void SetContenido(Integer proceso, Integer msj) {
		
		this.contenido = "P" 
						+ String.format("%02d", proceso)
						+ " " 
						+ String.format("%03d", msj);
		
	}
	
	public void SetOrden(Integer k) {
		
		this.orden = k;
		
	}
	
	public void SetPropuestas(Integer p) {
		
		this.numPropuestas = p;
		
	}
	
	//Metodos GET
	
	public Integer GetIdMensaje() {
		
		return this.idMensaje;
		
	}
	
	public Integer GetIdProceso() {
		
		return this.idProceso;
		
	}
	
	public Integer GetIdEquipo() {
		
		return this.idEquipo;
		
	}
	
	public String GetContenido() {
		
		return this.contenido;
		
	}
	
	public Integer GetOrden() {
		
		return this.orden;
		
	}
	
	public Integer GetPropuestas() {
		
		return this.numPropuestas;
		
	}
	
	public int GetEstado() {
		
		if(this.estado.compareTo(PROV) == 0)
			return 0;
		else
			return 1;
		
	}
	
}

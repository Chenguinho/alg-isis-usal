package classes;

public class Message {
	
	//Constantes
	
	static final String PROV = "PROVISIONAL";
	static final String DEF = "DEFINITIVO";
	
	//Atributos del mensaje
	
	Integer idMensaje, idEquipo, idProceso;
	Integer orden;
	Integer numPropuestas;
	
	String estado;
	
	String contenido;
	
	//Metodos creacion del mensaje
	
	public Message() {
		
	}
	
	public Message(Integer idM, Integer idP, Integer orden, Integer prop, int estado) {
		
		this.idMensaje = idM;
		this.idProceso = idP;
		
		this.numPropuestas = prop;
		this.orden = orden;
		
		SetEstado(estado);
		
		SetContenido(idP, idM);
		
	}
	
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
	
	public void SetEstado(int i) {
		
		if(i == 0)
			this.estado = PROV;
		else
			this.estado = DEF;
		
	}
	
	public void SetContenido(Integer proceso, Integer msj) {
		
		this.contenido = "P" 
						+ String.format("%02d", proceso)
						+ " " 
						+ String.format("%03d", msj);
		
	}
	
	public void SetOrden(Integer k) {
		
		this.orden = k;
		
	}
	
	public Integer SetOrdenLC1() {
		
		this.orden += 1;
		return orden;
		
	}
	
	public Integer SetOrdenLC2(Integer proceso) {
		
		int newOrden;
		
		if(proceso >= this.orden) {
			
			newOrden = proceso + 1;
			
		} else {
			
			newOrden = this.orden + 1;
			
		}
		
		this.orden = newOrden;
		
		return this.orden;
		
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

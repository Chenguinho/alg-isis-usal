package classes;

public class Message {
	
	static final String PROV = "PROVISIONAL";
	static final String DEF = "DEFINITIVO";
	
	String content, status;
	Integer id, computer, process, numPropuestas;
	long order; //k
	
	//Constructor
	public Message(Integer i, Integer c, Integer p, long k, int s) {
		this.id = i;
		this.computer = c;
		this.process = p;
		this.numPropuestas = 0;
		this.order = k;
		SetContent(this.id, this.process);
		SetStatus(s);
	}
	
	public Message() {
		
		this.numPropuestas = 0;
		
	}
	
	//Métodos constructor
	
	//SET -> Guardar valores
	
	public void SetContent(Integer i, Integer p) {
		this.content = "P" 
				+ String.format("%02d", p)
				+ " "
				+ String.format("%03d", i);
	}
	
	public void SetId(Integer i) {
		this.id = i;
	}
	
	public void SetProcess(Integer p) {
		this.process = p;
	}
	
	public void SetOrder(long k) {
		this.order = k;
	}
	
	public void SetStatus(int s) {
		if(s == 0 || s == 1) {
			if(s == 0) {
				this.status = PROV;
			} else {
				this.status = DEF;
			}
		}
	}
	
	public void SetPropuestas(int p) {
		this.numPropuestas = p;
	}
	
	//GET -> Recuperar información
	
	public Integer GetId() {
		return this.id;
	}
	
	public String GetContent() {
		return this.content;
	}
	
	public long GetOrder() {
		return this.order;
	}
	
	public Integer GetProcess() {
		return this.process;
	}
	
	public String GetStatus() {
		return this.status;
	}
	
	public Integer GetComputer() {
		return this.computer;
	}
	
	public Integer GetPropuestas() {
		return this.numPropuestas;
	}
}

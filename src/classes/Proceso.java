package classes;

import java.util.List;

public class Proceso {

	Mail mailService;
	
	int idProceso, idEquipo;
	List<String> vecinos;
	
	//Constructor
	public Proceso(int idProceso, int idEquipo, List<String> vecinos) {
		this.idProceso = idProceso;
		this.idEquipo = idEquipo;
		this.vecinos = vecinos;
	}
	
	//Métodos constructor
	
	//SET -> Guardar valores
	
	public void SetIdProceso(int i) {
		idProceso = i;
	}
	
	public void SetIdEquipo(int i) {
		idEquipo = i;
	}
	
	public void SetVecinos(List<String> v) {
		vecinos = v;
	}
	
	//GET -> Obtener valores
	
}

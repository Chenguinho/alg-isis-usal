package classes;

import java.util.concurrent.*;

public class Semaforo {

	Semaphore sem;
	
	public Semaforo(int nProc) {
		
		sem = new Semaphore(nProc);
		
	}
	
	public void Get() {
		
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void Drop() {
		
		sem.release();
		
	}
	
	public int AvailableSlots() {
		
		return sem.availablePermits();
		
	}
	
}

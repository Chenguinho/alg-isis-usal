package classes;

import java.util.concurrent.*;

public class Semaforo {

	Semaphore sem;
	
	public Semaforo(int nProc) {
		
		sem = new Semaphore(nProc);
		
	}
	
	public void acquire() {
		
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void release() {
		
		sem.release();
		
	}
	
	public void release(int nProc) {
		sem.release(nProc);
	}
	
	public int AvailableSlots() {
		
		return sem.availablePermits();
		
	}
	
}

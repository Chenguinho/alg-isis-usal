package helpers;

import java.util.concurrent.ThreadLocalRandom;

/*
 * La clase de apoyo Sleep es simplemente para reutilizar
 * la funcion de hacer dormir un tiempo aleatorio entre dos
 * valores al proceso
 */

public class Sleep {
	
	public void ThreadSleep(float min, float max) {
		
		double random = ThreadLocalRandom.current().nextDouble(min, max);
		random *= 1000;
		
		try {
			Thread.sleep((long) random);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}
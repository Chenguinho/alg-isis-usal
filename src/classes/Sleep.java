package classes;

import java.util.concurrent.ThreadLocalRandom;

public class Sleep {
	
	public void ThreadSleep(float min, float max) {
		
		double random = ThreadLocalRandom.current().nextDouble(min, max);
		
		try {
			Thread.sleep((long) random);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}

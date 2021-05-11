package helpers;

import java.util.concurrent.ThreadLocalRandom;

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
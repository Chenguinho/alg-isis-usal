package helpers;

import java.util.concurrent.ThreadLocalRandom;

public class Sleep {
	
	public void ThreadSleep(float min, float max) {
		
		double random = ThreadLocalRandom.current().nextDouble(min, max);
		
		try {
			Thread.sleep((long) random * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}
package isis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import classes.Message;

public class Multi extends Thread {

	private static final int MAX = 100;
	
	public void run() {
		
		List<Message> messagesList = new ArrayList<Message>();
		List<Integer> used = new ArrayList<Integer>();
		
		while(messagesList.size() < 100) {
			
			Integer randomNumber = ThreadLocalRandom.current().nextInt(1, 101);
	
			if(!used.contains(randomNumber)) {
				long threadId = Thread.currentThread().getId();
				
				String threadString = Long.toString(threadId);
				char char1 = threadString.charAt(threadString.length() - 2);
				char char2 = threadString.charAt(threadString.length() - 1);
				String thread = new StringBuilder(char1).append(char2).toString();
				
				Date date = new Date();
				
				messagesList.add(new Message(randomNumber, Integer.parseInt(thread), date.getTime(), 0));
				
				used.add(randomNumber);
				
				/*
				 * TODO
				 * 
				 * Multidifusion del mensaje y el sleep [1, 1.5]
				 */
			}
		}
		for (int i = 0; i < MAX; i++) {
			System.out.println(messagesList.get(i).GetContent());
		}
		
	}
	
}
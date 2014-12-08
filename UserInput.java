import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UserInput implements Runnable{
	public boolean USER_INPUT = false; 
	
	public void run(){
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String inputString;
        try {
			inputString = input.readLine();
			boolean run = true; 
			
			// wait for user input
			while(run) {
				if(inputString.equals("x")) {
					System.out.println("Closing...");
					
					run = false;
					setUserInput(true);
					}
			}
			System.exit(1);
        } catch (Exception e) {
        	System.out.println("Error waiting for user input: "+e.toString());
        	System.exit(1);
        }
	}
		
	public synchronized void setUserInput(boolean userInput){
		USER_INPUT = userInput;
	}
	
	public synchronized boolean isUserInput(){
		return USER_INPUT;
	}
	
}
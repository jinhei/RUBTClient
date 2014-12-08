
/**
 * RUBTClient project
 * @author Nicholas Fong and Selina Hui
 */

public class RUBTClient{
    
    public static void main(String[] args){
		String torrentFile = "";
		String outputFile = "";
		String ipArg = "";
	
		// Take as a command-line argument the name of the .torrent file to be loaded and the name of the file to save the data to. 
		if(args.length == 2 || args.length == 3) {
			torrentFile = args[0];
			outputFile = args[1];
		} else {
			System.out.println("Invalid Format.\n Ex. java -cp . RUBTClient [torrent file][save to file]");
			System.exit(1);
		} 
		if (args.length == 3)
			ipArg = args[2];
		
		UserInput userInput = new UserInput();
		System.out.println("Enter 'x' to exit at any time.");
		new Thread(userInput).start();
		
		Thread client = new Thread(new Client(torrentFile, outputFile, userInput, ipArg));
		client.start();
        
        while(!userInput.isUserInput()){
		}
        System.exit(1);
	}
    
    
}
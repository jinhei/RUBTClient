/**
 * Convenience wrapper containing utility methods
 * @author Nicholas Fong and Selina Hui
 */
import java.io.*;
import java.util.logging.*;

public class Utilities {
	
	/** 
	 * Reads fileName into byte[]
	 */
	public static byte[] fileByte(String fileName) throws IOException{ 
		File file = new File(fileName);
		byte[] bBuf = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		
		fis.read(bBuf);
		fis.close();
		return bBuf;
	}
	
	/**
	 * Converts byte[] to hex string
	 * source: http://stackoverflow.com/questions/4895523/java-string-to-sha1
	 */
	public static String byteToHexString(byte[] bArray){
		String hex = "";
		for (int i = 0; i < bArray.length; i++) {
			hex += "%"+Integer.toString( ( bArray[i] & 0xFF ) + 0x100, 16).substring( 1 ).toUpperCase();
		}
		return hex;
	}
	
	/**
	 * Generates empty file from of size filesize
	 */
	public static boolean generateFile(String outputFile, int filesize){
		try {
			FileOutputStream out = new FileOutputStream(outputFile);
			out.write(new byte[filesize]);
			out.close();
			return true;
		} catch (Exception e) {
			System.out.println("Error writing to file: "+e.toString());
			return false;
		}
	}
	
	/**
	 * Writes byte array to output file
	 */
	public static boolean writeToOutput(String outputFile, int offset, byte[] block, int filesize) throws IOException{
		RandomAccessFile output = null;
		try{
			output = new RandomAccessFile(outputFile, "rw");
		} catch (FileNotFoundException fnfe){
			generateFile(outputFile, filesize);
		}
		output.seek(offset);
		output.write(block);
		output.close();
		
		return false;
	}
	
	/**
	 * Initializes and returns a Logger for class className
	 * @param className
	 */
	public static Logger loggerInit(Class<?> c, boolean terminal){
		Logger log = Logger.getLogger(c.getName());
		FileHandler filename = null;
		
		try {
			filename = new FileHandler("RUBTClient.log");
		} catch (Exception e){
			System.out.println("Error creating logger");
			System.exit(1);
		}
		if(!terminal){
			Handler[] handlers = log.getHandlers();
			for(Handler handler : handlers) {
			    log.removeHandler(handler);
			}
		}
		log.addHandler(filename);
		SimpleFormatter formatter = new SimpleFormatter();
		filename.setFormatter(formatter);
		
		return log;
	}
	
	
}
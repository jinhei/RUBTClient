import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Logger;

public class Upload implements Runnable {
	public Peer peer;
	public Client client;
	public int index;
	public int offset;
	public int length;
	public byte[] block = null;
	
	public Logger log = null;
	
	public Upload(Peer peer, Client client, byte[] resp){
		this.peer = peer;
		this.client = client;
		log = client.log;
		
		index = ByteBuffer.wrap(Arrays.copyOfRange(resp, 5, 9)).getInt();
		offset = ByteBuffer.wrap(Arrays.copyOfRange(resp, 9, 13)).getInt();
		length = ByteBuffer.wrap(Arrays.copyOfRange(resp, 13, 17)).getInt();
		try {
			block = getBlockFromFile(); 
		} catch (Exception e) {
			//System.out.println("Error getting block to send");
			log.severe("Error getting block "+index+" with offset "+offset+" of length "+length+" to make piece message.");
		}
	}
	
	public byte[] getBlockFromFile() throws IOException{
		RandomAccessFile file = new RandomAccessFile(client.OUTPUT_FILE, "r");
		
		byte[] block = new byte[length];
		if(index < client.TOTAL_PIECES && offset < client.PIECE_LENGTH){
			file.seek(client.PIECE_LENGTH * index + offset);
			file.read(block);
			file.close();
		}
		
		return block;
	}
	
	public void run(){
		Message.sendMessage(Message.generateMessage("piece", index, offset, block), peer.output);
		client.UPLOAD_SPEED += block.length;
		client.UPLOADED += block.length;
		peer.downloaded += block.length;
	}
}
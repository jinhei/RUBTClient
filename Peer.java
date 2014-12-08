import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.logging.Logger;

/**
 * Convenience wrapper containing ip and port for a peer.
 * @author Nicholas Fong and Selina Hui
 */

public class Peer{
	public String peerID;
	public int port;
	public String ip;
	public ByteBuffer has = HAS_INIT(new byte[1]);
	private boolean isChoking = true;
	public Socket socket = null;
	public DataInputStream input = null;
	public DataOutputStream output = null;
	public final byte NO_BITFIELD = (byte) 2;
	private boolean isInterested = false;
	private boolean amChoking = false;
	public int downloaded = 0; 							/* amount that peer has downloaded */
	public int uploaded = 0; 							/* amount that peer has uploaded */
	public boolean connected = false;
	
	public Logger log = null;
	
	public Peer(){
	}
	
	public Peer(String peerID, int port, String ip, Client client){
		this.peerID = peerID;
		this.port = port;
		this.ip = ip;
		log = client.log;
	}
	
	/**
	 * Initialize Peer.has
	 */
	private ByteBuffer HAS_INIT(byte[] buf){
		buf[0] = NO_BITFIELD;
		return ByteBuffer.wrap(buf);
	}
	
	public synchronized void setHas(byte[] has){
		this.has = ByteBuffer.allocate(has.length);
		this.has = ByteBuffer.wrap(has);
	}
	
	public synchronized void choke(){
		this.isChoking = true;
	}
	public synchronized void unchoke(){
		this.isChoking = false;
	}
	public synchronized boolean isChoking(){
		return isChoking;
	}
	public synchronized void setInterested(boolean isInterested){
		this.isInterested = isInterested;
	}
	public synchronized boolean isInterested(){
		return isInterested;
	}
	public synchronized boolean amIChoking(){
		return amChoking;
	}
	public synchronized void amChoking(){
		if(!amChoking) {
			Message.sendMessage(Message.generateMessage("choke"), output);
		}
		amChoking = true;
	}
	public synchronized void amNotChoking(){
		if(amChoking) {
			Message.sendMessage(Message.generateMessage("unchoke"), output);
		}
		amChoking = false;
	}
	
	/**
	 * Returns list containing download speed, upload speed
	 * @param peer
	 * @return
	 */
	public List<Double> getSpeeds(){
		List<Double> speedList = new ArrayList<Double>();
		speedList.add(((double) downloaded) / 30);
		speedList.add(((double) uploaded) / 30);	
		downloaded = 0;
		uploaded = 0;
		
		return speedList;
	}
		
	/**
	 * Opens a socket and contacts peer with handshake
	 */
	public boolean handshake(byte[] info_hash, String peer_id) throws IOException{
		// open socket
		try { 
			socket = new Socket(this.ip, this.port);
			socket.setSoTimeout(30*1000); // timeout after 30 sec
		} catch(Exception e){
			log.warning("Error connecting to peer "+peerID+": "+e.toString());
			return false;
		} 
		
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		input = new DataInputStream(is);
		output = new DataOutputStream(os);
		
		//Send handshake and verify response
		try{
			output.write(Message.generateHandshake(info_hash, peer_id));
			output.flush();
			log.info("Sent peer handshake to peer "+peerID);
			
			byte[] peerResp = new byte[68];
			input.readFully(peerResp);
			log.info("Recieved peer handshake from peer "+peerID);
			if (!Message.verifyHandshake(info_hash, this, peerResp)){
				System.out.println("Peer "+peerID+" handshake returned incorrect info_hash");
				disconnect();
				return false;
			} else {
				connected = true;
				return true;
			}
			
		} catch (EOFException eofe) {
			log.warning("Peer "+peerID+" did not return handshake.");
			disconnect();
		}catch (Exception e){
			log.severe("Error sending handshake to peer "+peerID+": "+e.toString());
			disconnect();
		}
		
		return false;
	}
	
	/**
	 * Closes sockets (clean-up)
	 */
	public void disconnect(){
		try {
			socket.close();
			input.close();
			output.close();
			log.info("Disconnected from peer "+peerID);
		} catch (Exception e) {
		}
	}
}
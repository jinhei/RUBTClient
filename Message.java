import java.nio.ByteBuffer;
import java.util.Arrays;
//import java.util.logging.Logger;
import java.io.*;

/**
 * Convenience wrapper for message creation and parsing
 * @author Nicholas Fong and Selina Hui
 */

public class Message {
	
	public String type;
	public int length = 0; // payloadlength
	
	public static final byte TYPE_KEEP_ALIVE = -1;
	public static final byte TYPE_CHOKE = 0;
	public static final byte TYPE_UNCHOKE = 1;
	public static final byte TYPE_INTERESTED = 2;
	public static final byte TYPE_NOT_INTERESTED = 3;
	public static final byte TYPE_HAVE = 4;
	public static final byte TYPE_BITFIELD = 5;
	public static final byte TYPE_REQUEST = 6;
	public static final byte TYPE_PIECE = 7;
	public static final byte TYPE_CANCEL = 8;
	
	//public static Logger log = Utilities.loggerInit(Message.class);
	
	public Message(String type){
		this.type = type;
	}
	
	public Message(String type, int length){
		this.type = type;
		this.length = length;
	}
	
	/**
	 * Generates handshake
	 */
	public static byte[] generateHandshake(byte[] info_hash, String peer_id){
		//<byte 19><"BitTorrent protocol"><8 bytes of 0><info_hash from .torrent><generated peer_id>
		ByteBuffer buf = ByteBuffer.allocate(68);
		buf.put((byte) 19);
		buf.put("BitTorrent protocol".getBytes());
		buf.put(new byte[8]);
		buf.put(info_hash);
		buf.put(peer_id.getBytes());
		
		byte[] handshake = buf.array();
		
		return handshake;
	}
	
	/**
	 * Checks handshake info_hash and peer_id and verifies
	 */
	public static boolean verifyHandshake(byte[] info_hash, Peer peer, byte[] peerResp){
		int pstrlen = peerResp[0] & 0xff;
		
		//log.info("Verifying peer handshake...");
		if (!Arrays.equals(info_hash, Arrays.copyOfRange(peerResp, pstrlen+9, pstrlen+29))){
			//System.out.println("Peer handshake returned different info_hash.");
			/*log.info("Peer handshake returned different info_hash. Expected: "+Utilities.byteToHexString(info_hash)+
					"; received: "+Utilities.byteToHexString(Arrays.copyOfRange(peerResp, pstrlen+8, pstrlen+28)));*/
			return false;
		}
		if (!Arrays.equals(peer.peerID.getBytes(), Arrays.copyOfRange(peerResp, pstrlen+29, pstrlen+49))){
			//System.out.println("Peer handshake returned different peer_id");
			/*log.info("Peer handshake returned different peer_id. Expected: "+peer.peerID+"" +
					"; received: "+new String(Arrays.copyOfRange(peerResp, pstrlen+28, pstrlen+48)));*/
			return false;
		}
		//log.info("Peer handshake verified for peer "+peer.peerID);
		return true;
	}
	
	/**
	 * Sends message through output
	 */
	public static void sendMessage(byte[] message, DataOutputStream output){
		try{
			output.write(message);
			output.flush(); 
		} catch (Exception e){
			System.out.println("Error sending message: "+e.toString());
			//log.warning("Error sending message: "+e.toString());
		}
	}
	
	/**
	 * Receives message from datainputstream input
	 */
	public static byte[]receiveMessage(DataInputStream input){
		byte[] resp = new byte[5];
		try{
			input.readFully(resp);
		} catch (Exception e) {
			System.out.println("Error receiving message: "+e.toString());
			//log.severe("Error receiving message: "+e.toString());
		}
		
		Message respMessage = parseMessage(resp);
		//log.info("Got "+respMessage.type+" message from peer.");
		// Get payload if payload length was more than 0
		if(respMessage.length !=0 ){
			try {
				byte[] payload = getPayload(respMessage, input);
				ByteBuffer newResp = ByteBuffer.allocate(5+payload.length);
				newResp.put(resp);
				newResp.put(payload);
				return newResp.array();
			} catch (Exception e) {
				System.out.println("Error getting payload: "+e.toString());
				//log.severe("Error getting payload: "+e.toString());
			}
		} 
		return resp;
	}
	
	/**
	 * Parses message and returns message type and length of payload 
	 */
	public static Message parseMessage(byte[] resp){
		if (resp.length == 4){
			return new Message("keep-alive");
		}
		byte id = resp[4];
		
		// get length of payload 
		int pLength = ByteBuffer.wrap(Arrays.copyOfRange(resp,0,4)).getInt() -1;
		
		// return new message 
		switch (id) {
			case TYPE_CHOKE: return new Message("choke");
			case TYPE_UNCHOKE: return new Message("unchoke");
			case TYPE_INTERESTED: return new Message("interested");
			case TYPE_NOT_INTERESTED: return new Message("not interested");
			case TYPE_HAVE: return new Message("have", pLength);
			case TYPE_BITFIELD: return new Message("bitfield", pLength);
			case TYPE_REQUEST: return new Message("request", pLength);
			case TYPE_PIECE: return new Message("piece", pLength);
			case TYPE_CANCEL: return new Message("cancel", pLength);
			default: {
				System.out.println("Unknown message id "+id+" returned.");
				//log.warning("Unknown message id "+id+" returned.");
			}
		}
		return new Message("");
	}
	
	/**
	 * Process payload based on message type
	 */
	public static byte[] getPayload(Message respMessage, DataInputStream input) throws IOException{
		int respLength = respMessage.length;
		String respType = respMessage.type; 
		
		// Read payload into new buffer
		byte[] payload = new byte[respLength];
		input.readFully(payload);
		
		switch(respType) {
			case "have":{ // not handling have
				return payload;
			} case "bitfield":{
				// convert bitfield to boolean array
				boolean[] bitfield = BitToBoolean.convert(payload); 
				int totalPieces = bitfield.length;
				
				// convert boolean array to byte[]
				byte[] peerHas = new byte[totalPieces];
				for (int i = 0; i < totalPieces; i++){
					if(bitfield[i]){
						peerHas[i] = (byte) 1;
					}
				}
				return peerHas;
			} case "request":{ 
				return payload;
			} case "piece":{
				return payload;
			} case "cancel":{ 
			} 
		}
		return new byte[4];
	}
	
	/**
	 * Generates message for keep-alive, choke, unchoke, interested, not interested
	 */
	public static byte[] generateMessage(String type){
		switch (type) {
			case "keep-alive":
				return new byte[1]; // returns a single byte containing a 0
			case "choke":{
				ByteBuffer buf = ByteBuffer.allocate(5);
				buf.putInt(1);
				buf.put(TYPE_CHOKE);
				return buf.array();
			} case "unchoke":{
				ByteBuffer buf = ByteBuffer.allocate(5);
				buf.putInt(1);
				buf.put(TYPE_UNCHOKE);
				return buf.array();
			} case "interested":{
				ByteBuffer buf = ByteBuffer.allocate(5);
				buf.putInt(1);
				buf.put(TYPE_INTERESTED);
				return buf.array();
			} case "not interested":{
				ByteBuffer buf = ByteBuffer.allocate(5);
				buf.putInt(1);
				buf.put(TYPE_NOT_INTERESTED);
				return buf.array();
			} default:{
				System.out.println("Error: generateMessage of type "+type+" has incorrect parameters");
				//log.warning("Error: generateMessage of type "+type+" has incorrect parameters");
			}
		}
		
		return new byte[1];
	}
	
	/**
	 * generateMessage overloaded for HAVE messages
	 */
	public static byte[] generateMessage(String type, int index){
		if(!type.equals("have")){
			System.out.println("Error: generateMessage of type "+type+" has incorrect parameters");
			//log.warning("Error: generateMessage of type "+type+" has incorrect parameters");
		}
		ByteBuffer buf = ByteBuffer.allocate(9);
		buf.putInt(5);
		buf.put(TYPE_HAVE);
		buf.putInt(index);
		return buf.array();
	}
	
	/**
	 * generateMessage overloaded for REQUEST messages
	 */
	public static byte[] generateMessage(String type, int index, int begin, int length) {
		if(!type.equals("request")){
			System.out.println("Error: generateMessage of type "+type+" has incorrect parameters");
			//log.warning("Error: generateMessage of type "+type+" has incorrect parameters");
		} 
		ByteBuffer buf = ByteBuffer.allocate(17);
		buf.putInt(13);
		buf.put(TYPE_REQUEST);
		buf.putInt(index);
		buf.putInt(begin);
		buf.putInt(length);
		return buf.array(); 
	}
	
	/**
	 * generateMessage overloaded for PIECE messages
	 */
	public static byte[] generateMessage(String type, int index, int begin, byte[] block) {
		if(!type.equals("piece")){
			System.out.println("Error: generateMessage of type "+type+" has incorrect parameters");
			//log.warning("Error: generateMessage of type "+type+" has incorrect parameters");
		}

		int length = block.length;
		ByteBuffer buf = ByteBuffer.allocate(7+length);
		buf.putInt(9+length);
		buf.put(TYPE_PIECE);
		buf.putInt(index);
		buf.putInt(begin);
		buf.put(block);
		return buf.array(); 
	}
}
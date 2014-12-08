import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;

/**
 * Convenience wrapper for tracker related functions
 * @author Nicholas Fong and Selina Hui
 */

public class Tracker {

	public static final ByteBuffer KEY_PEERS = ByteBuffer.wrap("peers".getBytes());
	public static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap("peer id".getBytes());
	public static final ByteBuffer KEY_PORT = ByteBuffer.wrap("port".getBytes());
	public static final ByteBuffer KEY_IP = ByteBuffer.wrap("ip".getBytes());
	public static final ByteBuffer KEY_FAILURE_REASON = ByteBuffer.wrap("failure reason".getBytes());
	public static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap("interval".getBytes());
	public static final ByteBuffer KEY_STARTED = ByteBuffer.wrap("started".getBytes());
	public static final ByteBuffer KEY_STOPPED = ByteBuffer.wrap("stopped".getBytes());
	public static final ByteBuffer KEY_COMPLETED = ByteBuffer.wrap("completed".getBytes());
	
	public static List<Peer> newPeers(byte[] resp, Client client) throws BencodingException{
		List<Peer> newPeers = new ArrayList<Peer>();
		List<Peer> newPeerList = getPeerList(resp, client);
		
		boolean isOld = false;
		List<Peer> currentPeerList = client.getPeerList();
		for(Peer peer : newPeerList){
			isOld = false;
			for(Peer oldPeer : currentPeerList){
				if(peer.ip.equals(oldPeer.ip)){
					isOld = true;
				}
			}
			if(!isOld){
				newPeers.add(peer);
			}
		}
		
		return newPeers;
	}
	
	/**
	 * Returns peer list from tracker response
	 * @param resp tracker response
	 * Based on PowerPoint slides from CS 352 Spring 2010 created by TA Tuan Phan (Jan 28, 2010)
	 */
	public static List<Peer> getPeerList(byte[] resp, Client client) throws BencodingException{
		HashMap<ByteBuffer,Object> respMap = (HashMap<ByteBuffer, Object>) Bencoder2.decode(resp);
		
		if (respMap.containsKey(KEY_FAILURE_REASON)){
			client.log.severe("Tracker returned failure reason: "+(String) respMap.get(KEY_FAILURE_REASON));
		}
		
		ArrayList<HashMap<ByteBuffer,Object>> peerArrayList = (ArrayList<HashMap<ByteBuffer, Object>>) respMap.get(KEY_PEERS);
		
		// Populate peerList with peers
		List<Peer> peerList = new ArrayList<Peer>();
		for(HashMap<ByteBuffer,Object> peers : peerArrayList) {
			String peer_id = new String(((ByteBuffer) peers.get(KEY_PEER_ID)).array()); 
			int port = ((Integer) peers.get(KEY_PORT)).intValue(); 
			String ip = new String(((ByteBuffer) (peers.get(KEY_IP))).array());
			peerList.add(new Peer(peer_id, port, ip, client));
		}
		
		return peerList;
	}
	
	/**
	 * Returns interval from tracker response
	 * @param resp tracker response
	 */
	public static int getInterval(byte[] resp) throws BencodingException{
		int interval = 0;
		HashMap<ByteBuffer,Object> respMap = (HashMap<ByteBuffer, Object>) Bencoder2.decode(resp);
		
		if (respMap.containsKey(KEY_FAILURE_REASON)){
			System.out.print("Tracker returned failure reason: "+(String) respMap.get(KEY_FAILURE_REASON));
			// log.info("Tracker returned failure reason: "+(String) respMap.get(KEY_FAILURE_REASON));
		}
		interval = (Integer) respMap.get(KEY_INTERVAL);
		
		return interval;
	}
	
	/**
	 * Connects to tracker and returns the tracker response 
	 * @param 
	 */
	public static byte[] getTrackerResp(URL url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setConnectTimeout(5000);
		DataInputStream dis = new DataInputStream(con.getInputStream());
		byte[] trackerResp = new byte[con.getContentLength()];
		dis.readFully(trackerResp);
		dis.close();
		
		return trackerResp;
	}
	
	
	/**
	 * Generates a random peer id
	 */
	public static String generatePeerID(){
		char[] charArray = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
		String peer_ID = "";
		Random random = new Random();
		for (int i = 0; i < 20; i++){
			peer_ID += charArray[random.nextInt(26)];
		}
		
		return peer_ID;
	}
	
}
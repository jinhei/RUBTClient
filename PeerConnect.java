import java.util.Timer;
import java.util.logging.Logger;


/**
 * Thread that connects to peer, handshakes
 * @author Nicholas Fong and Selina Hui
 */
public class PeerConnect implements Runnable {
	
	public Peer peer;
	public Client client;
	private boolean sentUnchoke = false;
	
	public Logger log = null;
	
	public void keepAlive(Peer peer){
		Timer timer = new Timer();
		timer.schedule(new KeepAlive(peer), 120*1000, 120*1000);
	}
	
	public PeerConnect(Peer peer, Client client){
		this.peer = peer;
		this.client = client;
		log = client.log;
	}
	
	public void sendInterested(){
		if(client.amIInterested()){
			Message.sendMessage(Message.generateMessage("interested"), peer.output);
			log.info("Sent interested message to peer "+peer.peerID);
		} 
	}
	
	public void run(){
		try {
			// handshake
			if(peer.handshake(client.INFO_HASH, client.PEER_ID)){
				log.info("Verified handshake with peer "+peer.peerID);
				sendInterested();
				keepAlive(peer);
				while(!client.USER_INPUT.isUserInput()){
					//log.info("Getting index...");
					int index = -1;
					if((index = client.getNextPieceIndex(peer.has)) == -1){
						peer.disconnect();
						break;
					} else{
						int[] havePieces = client.getHavePieces();
						// 	Send request
						while(!(peer.isChoking() || client.isCompleted() || client.USER_INPUT.isUserInput() || peer.amIChoking())){
							//log.info("Peer is not choked. Index at "+index);
							/*if(!sentUnchoke){
								Message.sendMessage(Message.generateMessage("unchoke"), peer.output);
								log.info("Sent unchoke message to peer "+peer.peerID);
								sentUnchoke = true;
							}*/
							if(peer.has.get(0) == peer.NO_BITFIELD || (peer.has.get(index)==(byte) 1 && havePieces[index]==0)){ // if peer has this file
								log.info("Sending request for piece "+index+" to peer "+peer.peerID);
								Message.sendMessage(Message.generateMessage("request", index, (int) 0, client.getPieceLength(index)), peer.output);
								byte[] peerResp = Message.receiveMessage(peer.input);
								client.processMessage(peerResp, peer, index);
								//log.info("Processed message.");
							}
							index = client.getNextPieceIndex(peer.has);
							havePieces = client.getHavePieces();
						}
						//	Receive Messages from peer to look for unchoke
						while(peer.isChoking() && !client.USER_INPUT.isUserInput() && !peer.amIChoking()){
							log.info("Waiting for peer "+peer.peerID+" to unchoke...");
							byte[] peerResp = Message.receiveMessage(peer.input);
							client.processMessage(peerResp, peer, index);
						}
					}
				} 
			} else {
				log.info("Handshake failed with peer "+peer.peerID+" at "+peer.ip+":"+peer.port);
			}
			peer.connected = false;
			peer.disconnect();	
			client.RemoveFromPeerList(peer);
			
		} catch (Exception e){
			//System.out.println("Error: exception "+e.toString());
			log.severe("Peer connect error: exception "+e.toString());
			peer.connected = false;
			peer.disconnect();
		}
		
		peer.disconnect();
	}
}
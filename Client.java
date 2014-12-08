import GivenTools.TorrentInfo;
import GivenTools.BencodingException;

import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.*;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Thread that does all the client stuff
 * @author Nicholas Fong and Selina Hui
 */
public class Client implements Runnable{
	public UserInput USER_INPUT = null;
	public TorrentInfo TORRENT_INFO = null;
	public String OUTPUT_FILE = null;
	public String STATS_FILE = "RUBTClient_stats.log";
	private List<Peer> PEER_LIST = new ArrayList<Peer>();
	
	public int TOTAL_DOWNLOADED = 0;
	public int TOTAL_UPLOADED = 0;
	public int FILE_SIZE = 0;
	public int PORT = 6881;
	public byte[] INFO_HASH;
	public String PEER_ID = null;
	public int INTERVAL = 5000;
	public int PIECE_LENGTH = 16384;
	public int REQUEST_PIECE_LENGTH;
	public int TOTAL_PIECES = 0;
	public ByteBuffer PIECE_HASHES[] = null;
	public int ID_LOC = 4;
	
	private long START_TIME = -1;
	private long END_TIME = -1;
	public int UPLOADED = 0;
	public int DOWNLOADED = 0;
	public double UPLOAD_SPEED = 0;
	public double DOWNLOAD_SPEED = 0;
	public int TIME_ELAPSED = 0;
	private boolean STARTED = false;
	private boolean STOPPED = false;
	private boolean COMPLETED = false;
	private int LEFT = 0;
	private int[] HAVE_PIECES = null;
	private int[] PIECE_RARITY = null;
	private int PIECES_DOWNLOADED = 0;
	private boolean amInterested = true;
	private int UNCHOKED_PEERS = 0;
	private int TOTAL_PEERS = 0;
	
	private static JTable infoTab;
    private static JTable trackerTab;
    private static JTable peerTab;
    private static JProgressBar progress;

    public Logger log = null;
	
	public boolean LOG_TO_TERMINAL = true; 
	public String IP_ARGUMENT = "";
	public String TEST_IP_1 = "128.6.171.130";
	public String TEST_IP_2 = "128.6.171.131";
	
	public Client(String torrentFile, String outputFile, UserInput userInput, String ipArg){
		log = Utilities.loggerInit(this.getClass(), LOG_TO_TERMINAL);
		USER_INPUT = userInput;
		IP_ARGUMENT = ipArg;
		// Open the .torrent file and create a buffer to hold the output file
		try{
			TORRENT_INFO = new TorrentInfo(Utilities.fileByte(torrentFile));
			processTorrentInfo(TORRENT_INFO);
		} catch (Exception e){
			//System.out.println("Error opening torrent. Exception: " +e.toString());
			System.exit(1);
		}
		OUTPUT_FILE = outputFile;
	}
	
	public synchronized boolean isCompleted(){
		return COMPLETED;
	}
	
	
	public synchronized int getUnchokedPeers(){
		return UNCHOKED_PEERS;
	}
	public synchronized void setUnchokedPeers(int i){
		UNCHOKED_PEERS = i;
	}
	public synchronized int getTotalPeers(){
		TOTAL_PEERS = PEER_LIST.size();
		return TOTAL_PEERS;
	}
	public synchronized List<Peer> getPeerList(){
		return PEER_LIST;
	}
	public synchronized void addToPeerList(Peer peer){
		PEER_LIST.add(peer);
		TOTAL_PEERS = PEER_LIST.size();
	}
	public synchronized void RemoveFromPeerList(Peer peer){
		PEER_LIST.remove(peer);
		TOTAL_PEERS = PEER_LIST.size();
	}
	
	public synchronized void startPeerConnections(){
		List<Peer> peer_list = getPeerList();
		for (Peer peer : peer_list){
			log.info("Peer "+peer.peerID+": IP "+peer.ip+":"+peer.port);
			Thread peerConnect = new Thread(new PeerConnect(peer, this));
			peerConnect.start(); 
		}
	}
	
	public synchronized int[] getHavePieces(){
		return HAVE_PIECES;
	}
	public synchronized void setHavePieces(int i, int value){
		HAVE_PIECES[i] = value;
	}
	public synchronized boolean haveAllPieces(){ 
		for(int pieces : HAVE_PIECES){
			if(pieces == 0){
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Updates PIECE_RARITY
	 */
	public synchronized void updateRarity(){
		PIECE_RARITY = new int[TOTAL_PIECES];
		List<Peer> peer_list = getPeerList();
		for (Peer peer : peer_list){
			for (int i = 0; i < TOTAL_PIECES; i++){
				if (peer.has.get(0) == peer.NO_BITFIELD){
					break;
				} else if (peer.has.get(i) == (byte) 1){
					PIECE_RARITY[i] += 1;
				}
			}
		}
	}
	
	/**
	 * Checks HAVE_PIECES and returns the next index to be requested based on rarity 
	 * @param peer_has
	 */
	public synchronized int getNextPieceIndex(ByteBuffer peer_has){
		int[] have_pieces = getHavePieces(); 
		if(peer_has.get(0) == new Peer().NO_BITFIELD){
			for(int i = 0; i < TOTAL_PIECES; i++){
				if(have_pieces[i] == 0){
					return i;
				}
			}
		}
		updateRarity();
		int rarest = 0;
		
		/* Checks for rarest piece that peer has and client does not */
		for (int i = 0; i < TOTAL_PIECES; i++) {
			if (rarest == 0 || (PIECE_RARITY[i] < rarest && have_pieces[i] == 1 && peer_has.get(i) == (byte) 1)) {
				rarest = PIECE_RARITY[i];
			}
		}
		
		if (rarest == 0) {
			return -1;
		}
		
		List<Integer> possiblePieces = new ArrayList<Integer>();
		for (int i = 0; i < TOTAL_PIECES; i++) {
			if (PIECE_RARITY[i] == rarest) {
				possiblePieces.add(i);
			}
		}
		
		if (possiblePieces.size() > 1) {
			return possiblePieces.get((new Random().nextInt(possiblePieces.size())));
		} else if (possiblePieces.size() == 0) {
			return -1;
		} else {
			return possiblePieces.get(1);
		}
	}

	/**
	 * Calculates and returns number of pieces downloaded
	 */
	public int getPiecesDownloaded(){
		int []have_pieces = getHavePieces(); 
		PIECES_DOWNLOADED = 0;
		if (have_pieces == null)
			return 0;
		else {
			for (int i = 0; i < have_pieces.length; i++){
				if(have_pieces[i] == 1)
					PIECES_DOWNLOADED++;
			}
			return PIECES_DOWNLOADED;
		}
	}
	
	public void updatePeerSpeed(){
		Timer timer = new Timer();
		timer.schedule(new SpeedUpdate(getPeerList(), this), 0, 30*1000);
	}
	
	public void updateClientSpeed() {
		Timer timer = new Timer();
		timer.schedule(new ClientSpeedUpdate(this), 0, 1*1000);
	}
	
	
	public synchronized boolean amIInterested(){
		return amInterested;
	}
	public synchronized void amInterested(){
		amInterested = true;
	}
	public synchronized void amNotInterested(){
		amInterested = false;
	}
	
	/**
	 * Returns piece length of piece (index)
	 * @param index
	 */
	public int getPieceLength(int index){
		if((FILE_SIZE - index*PIECE_LENGTH) < PIECE_LENGTH && FILE_SIZE%PIECE_LENGTH != 0 ){
			return FILE_SIZE%PIECE_LENGTH;
		} else return PIECE_LENGTH;
	}
	
	
	private void initialize(){
		PEER_ID = Tracker.generatePeerID();
		try {
			readStats();
			HAVE_PIECES = checkOutputFile();
			if(getPiecesDownloaded() == TOTAL_PIECES){
				amNotInterested();
			} else amInterested();
				
		} catch (IOException e) {
			log.severe("Error verifying output file pieces: "+e.toString());
		}
		
		Thread GUI = new Thread(new Runnable(){
            public void run()
            {
                GUI();
            }
        });
		GUI.start();
	}
	
	public void run(){
		initialize();
		
		if(!isCompleted()){
			START_TIME = System.nanoTime();
		}
		
		// Connect to tracker
		log.info("Connecting to tracker...");
		trackerConnect();
		log.info("Connected to tracker.");
		
		// Print peerlist and start connection
		startPeerConnections();
		updatePeerSpeed();
		
		// Tell tracker download is starting
		STARTED = true;
		try {
			Tracker.getTrackerResp(generateURL());
		} catch (Exception e) {
			//System.out.println("Error sending tracker STARTED:" +e.toString());
			log.warning("Error sending tracker STARTED: "+e.toString());
		}
		
		System.out.println("Enter x to exit.");
		
		// update tracker
		List<Peer> newPeers = null;
		while(!(isCompleted() || USER_INPUT.isUserInput())){
			try {
				Thread.sleep((long) INTERVAL);
				byte[] trackerResp = Tracker.getTrackerResp(generateURL());
				if(!(newPeers = Tracker.newPeers(trackerResp, this)).isEmpty()){
					for(Peer peer : newPeers){
						addToPeerList(peer);
						log.info("Peer "+peer.peerID+": IP "+peer.ip+":"+peer.port);
						Thread peerConnect = new Thread(new PeerConnect(peer, this));
						peerConnect.start();
					}
					newPeers = null;
				}
			} catch (Exception e) {
				//System.out.println("Error updating tracker: "+ e.toString());
				log.warning("Error updating tracker: "+e.toString());
			}
		} 
		
		// send completed 
		if (isCompleted() && !USER_INPUT.isUserInput()){
			try {
				Tracker.getTrackerResp(generateURL());
			} catch (Exception e) {
				//System.out.println("Error sending tracker complete.");
				log.warning("Error sending tracker complete.");
			} 
		}
		
		System.out.println("Enter x to exit.");
		
		// keep updating tracker
		while(!USER_INPUT.isUserInput()){
			try {
				Thread.sleep((long) INTERVAL);
				byte[] trackerResp = Tracker.getTrackerResp(generateURL());
				if(!(newPeers = Tracker.newPeers(trackerResp, this)).isEmpty()){
					for(Peer peer : newPeers){
						addToPeerList(peer);
						if(!IP_ARGUMENT.isEmpty()){
							if(peer.ip.equals(IP_ARGUMENT)){
								log.info("Peer "+peer.peerID+": IP "+peer.ip+":"+peer.port);
								Thread peerConnect = new Thread(new PeerConnect(peer, this));
								peerConnect.start(); 
							}
						} else if(peer.ip.equals(TEST_IP_1) || peer.ip.equals(TEST_IP_2)){
							log.info("Peer "+peer.peerID+": IP "+peer.ip+":"+peer.port);
							Thread peerConnect = new Thread(new PeerConnect(peer, this));
							peerConnect.start();
						}
					}
					newPeers = null;
				}
			} catch (Exception e) {
				//System.out.println("Error updating tracker: "+ e.toString());
				log.warning("Error updating tracker: "+e.toString());
			}
		}
		try {
			STOPPED = true;
			Tracker.getTrackerResp(generateURL());
		} catch (Exception e) {
			log.warning("Error sending tracker stopped");
		}
		writeStats();
	}
	
	/**
	 * Connects to tracker between port 6881 and 6889, parses response and saves peerlist and interval values
	 */
	public void trackerConnect(){
		byte[] trackerResp = null;
		while(true){
			try {
				// Contact tracker and get peer list
				trackerResp = Tracker.getTrackerResp(generateURL());
				PEER_LIST = Tracker.getPeerList(trackerResp, this);
				INTERVAL = Tracker.getInterval(trackerResp);
				break;
			} catch (MalformedURLException e){
				//System.out.println("Error generating URL.");
				log.severe("Exception "+e.toString());
				System.exit(1);
			} catch (ConnectException e){
				// Connection Error tries next port
				//System.out.println("Error connecting on port "+PORT+". Retrying on port "+(++PORT)+"...");
				log.info("Error connecting on port "+PORT+". Retrying on port "+(++PORT)+"...");
			} catch (BencodingException e){
				//System.out.println("Bencoding error.");
				log.severe("Exception "+e.toString());
				System.exit(1);
			} catch (Exception e){
				//System.out.println("URL/tracker error: "+e.toString());
				log.severe("Exception "+e.toString());
				System.exit(1);
			}
			
			// Stops trying ports after 6889
			if(PORT > 6889){
				//System.out.println("Error connecting to tracker.");
				log.info("Ran of ports. Error connecting to tracker.");
				System.exit(1);
			}
		}	
	}

	/**
	 * parses the message and processes the payload if applicable.
	 */
	public synchronized void processMessage(byte[] resp, Peer peer, int expected_index) throws IOException, NoSuchAlgorithmException, IllegalThreadStateException{
		int[] have_pieces = getHavePieces();
		switch ((resp[ID_LOC])) {		
			case (Message.TYPE_BITFIELD): { 					// BITFIELD
				log.info("Recieved bitfield from peer "+peer.peerID+".");
				byte[] has = Arrays.copyOfRange(resp, 5, resp.length);
				if(has.length < TOTAL_PIECES){
					log.info("Bitfield only "+has.length+" total pieces rather than "+TOTAL_PIECES+".");
				} else { 
					peer.setHas(has);
				}
				break;
			}
			case (Message.TYPE_PIECE): {						// PIECE
				log.info("Recieved piece from peer "+peer.peerID+".");
				int index = ByteBuffer.wrap(Arrays.copyOfRange(resp, 5, 9)).getInt();
				int offset = ByteBuffer.wrap(Arrays.copyOfRange(resp, 9, 13)).getInt();
				byte[] block = Arrays.copyOfRange(resp, 13, resp.length);
				
				if(have_pieces[expected_index] == 1){
					break; // ignore this piece if we already have it
				}
				else if(verifySHA1(index, block, expected_index)){
					int piece_length;
					
					// check if last piece
					if(expected_index == TOTAL_PIECES && PIECE_LENGTH%TOTAL_PIECES != 0){
						piece_length = PIECE_LENGTH%TOTAL_PIECES;
					} else {
						piece_length = PIECE_LENGTH;
					}
					
					// Put piece into position
					try{
						Utilities.writeToOutput(OUTPUT_FILE,expected_index*PIECE_LENGTH+offset, block, FILE_SIZE);
						log.info("Wrote block "+expected_index+" to "+OUTPUT_FILE);
					} catch (Exception e) {
						//System.out.println("Error putting to OUTPUT_BUFFER: " +e.toString());
						log.severe("Error putting block "+expected_index+" to "+OUTPUT_FILE+": "+e.toString());
						System.exit(1);
					}
					DOWNLOADED += piece_length;
					DOWNLOAD_SPEED += piece_length;
					peer.uploaded += piece_length;
					LEFT = FILE_SIZE - DOWNLOADED;
					setHavePieces(index, 1);
					Message.sendMessage(Message.generateMessage("have", expected_index), peer.output);
					log.info("Sent have for piece "+expected_index+" to peer "+peer.peerID);
					
					if (haveAllPieces()) {
						COMPLETED = true;
						END_TIME = System.nanoTime();
						log.info("Download complete for file "+TORRENT_INFO.file_name+" complete.");
						printTimeElapsed();
					}
				} else {
					log.info("Got bad piece from peer "+peer.peerID);
				}
				break;
			} 	
			case (Message.TYPE_UNCHOKE): {						// UNCHOKE
				log.info("Recieved unchoke from peer "+peer.peerID+".");
				peer.unchoke();
				break;
			}
			case (Message.TYPE_CHOKE): {						// CHOKE
				log.info("Recieved choke from peer "+peer.peerID+".");
				peer.choke();
				break;
			}
			case (Message.TYPE_REQUEST): {						// REQUEST
				log.info("Recieved request from peer "+peer.peerID+". Creating upload thread.");
				Thread upload = new Thread(new Upload(peer, this, resp));
				upload.start();
				break;
			} 
			case (Message.TYPE_HAVE): {							// HAVE
				int index = (int) resp[5];
				log.info("Got peer "+peer.peerID+" has index "+index);
				peer.has.put(index, (byte) 1);
				break;
			} 
			case (Message.TYPE_INTERESTED): {					// INTERESTED
				log.info("Recieved interested from peer "+peer.peerID+".");
				//Message.sendMessage(Message.generateMessage("unchoke"), peer.output);
				peer.setInterested(true);
			}
			default: 											// OTHER (ignore)
				log.info("Got other message with id "+(int)resp[ID_LOC]);
		}
	}
	
	/**
	 * Checks the SHA-1 hash of a block against the given SHA-1 from .torrent file 
	 * @param resp
	 * @param check_index
	 */
	public boolean verifySHA1(int index, byte[] block, int check_index){
		log.info("Verifying block "+index+" against .torrent hash "+check_index);
		
		// sha1 hash block
		MessageDigest sha1 = null;
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			//System.out.println("SHA-1 algorithm not found.");
			log.severe("SHA-1 algorithm not found: "+e.toString());
			System.exit(1);
		}
		String metadataHash = Utilities.byteToHexString(PIECE_HASHES[index].array());
		String pieceHash = Utilities.byteToHexString(sha1.digest(block));
		
		// Verify piece
		if(index != check_index){
			log.info("Recieved wrong piece "+index+"; expected "+check_index);
			return false;
		} else if (block.length != getPieceLength(check_index)){
			log.info("Piece of unexpected size at index "+index);
			return false;
		} else if (!metadataHash.equals(pieceHash)){
			log.info("Piece at index "+index+" returned different SHA-1 hash. ");
			return false;
		} else {
			log.info("SHA-1 match for piece "+index);
			setHavePieces(index, 1);
			DOWNLOADED += block.length;
			return true;
		}
	}
	
	/**
	 * Checks and verifies output file pieces and returns array of downloaded pieces
	 */
	public int[] checkOutputFile() throws IOException {
        int lastPieceSize = 0;
        int[] verifiedPieces = new int[TOTAL_PIECES];
        byte[] pieceArray = null;
        RandomAccessFile outputFileCheck = null;
        
        try{
        	outputFileCheck = new RandomAccessFile(OUTPUT_FILE, "r");
        	if(outputFileCheck.length() != (long)FILE_SIZE){
        		outputFileCheck.close();
        		throw new FileNotFoundException();
        	}
        } catch (FileNotFoundException fnfe) {
        	log.info("No previous file found. Generating new output file");
        	Utilities.generateFile(OUTPUT_FILE, FILE_SIZE);
        	return verifiedPieces;
        }
 
        if(FILE_SIZE % TOTAL_PIECES == 0)
            lastPieceSize = PIECE_LENGTH;
        else
            lastPieceSize = FILE_SIZE % PIECE_LENGTH;
         
        for (int index = 0; index < TOTAL_PIECES; index ++)
        {
            //Check if piece is in output file byte array
            if(index < TOTAL_PIECES - 1)
            {
                pieceArray = new byte[PIECE_LENGTH];
                outputFileCheck.seek(PIECE_LENGTH * index + 0);
                outputFileCheck.read(pieceArray);
            }
            else
            {
                pieceArray = new byte[lastPieceSize];
                outputFileCheck.seek(PIECE_LENGTH * index + 0);
                outputFileCheck.read(pieceArray);
            }
             
            //Verify SHA1
            if(verifySHA1(index, pieceArray, index))
                    verifiedPieces[index] = 1;
        }
        
        outputFileCheck.close();
        return verifiedPieces;
    }
	
	/**
	 * Generates url
	 */
	public URL generateURL() throws MalformedURLException{
		INFO_HASH = TORRENT_INFO.info_hash.array();
		String uploaded = String.valueOf(UPLOADED);
		String downloaded = String.valueOf(DOWNLOADED);
		String left = String.valueOf(LEFT);
		
		// Create URL from pieces
		String url = TORRENT_INFO.announce_url.toString()+
				"?info_hash="+Utilities.byteToHexString(INFO_HASH)+"&peer_id="+PEER_ID+"&port="+PORT+"&uploaded="+uploaded+
				"&downloaded="+downloaded+"&left="+left;
		//log.info("Generated URL "+url);
		
		if(STARTED){
			STARTED = false;
			url += "&event=started";
		} else if(STOPPED){
			url += "&event=stopped";
		} else if(COMPLETED){
			url += "&event=completed";
		}
		
		return new URL(url);
	}
	
	public synchronized void readStats(){
		Scanner inputFile = null;
		 
        try {
			inputFile = new Scanner(new BufferedReader(new FileReader(STATS_FILE)));
	         
	        inputFile.skip("Uploaded:");
	        TOTAL_UPLOADED = inputFile.nextInt();
	        log.info("Uploaded: "+TOTAL_UPLOADED);
	         
	        inputFile.nextLine();
	         
	        inputFile.skip("Downloaded:");
	        TOTAL_DOWNLOADED = inputFile.nextInt();
	        log.info("Downloaded: "+TOTAL_DOWNLOADED);
	        
	        inputFile.close();
		} catch (FileNotFoundException e) {
			
		}
	}
	
	public void printTimeElapsed(){
		if(START_TIME != -1 && END_TIME != -1){
			float time_elapsed = (float) (END_TIME - START_TIME)/1000000000; 
			//System.out.println("Download time was "+time_elapsed+" seconds.");
			log.info("Download time was "+time_elapsed+" seconds.");
		}
	}
	
	public synchronized void writeStats(){
		TOTAL_UPLOADED += UPLOADED;
		TOTAL_DOWNLOADED += DOWNLOADED;
		try {
            BufferedWriter writeOut = new BufferedWriter(new FileWriter(STATS_FILE));
            writeOut.write ("Uploaded:"+ TOTAL_UPLOADED);
            writeOut.newLine();
            writeOut.write("Downloaded:"+ TOTAL_DOWNLOADED);
            log.info("Wrote total upload and download to RUBTClient_stats.log");
            writeOut.close();
        } catch(IOException e){
            System.out.println("Error writing stats to file");
            log.severe("Error writing stats to file");
            System.exit(1);
        }
	}
	
	/**
	 * Reads out values from TorrentInfo
	 */
	public void processTorrentInfo(TorrentInfo torrentInfo){
		log.info("Processing TorrentInfo.");
		FILE_SIZE = torrentInfo.file_length;
		LEFT = torrentInfo.file_length;
		PIECE_LENGTH = torrentInfo.piece_length;
		REQUEST_PIECE_LENGTH = PIECE_LENGTH;
		PIECE_HASHES = torrentInfo.piece_hashes;
		TOTAL_PIECES = torrentInfo.piece_hashes.length;
		HAVE_PIECES = new int[TOTAL_PIECES];
	}
	
	@SuppressWarnings("serial")
	public void GUI()
    {
        JFrame window = new JFrame("RUBTClient-Group 3");
        window.setVisible(true);
        window.setTitle("RUBTClient-Group3");
        window.setBounds(100, 100, 800, 600);
        window.getContentPane().setLayout(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBounds(10,11,600,500);
        window.getContentPane().add(tabbedPane);
        
        infoTab = new JTable();
        infoTab.setFont(new Font("Tohama", Font.PLAIN,15));
        infoTab.setModel(new DefaultTableModel
                         (
                          new Object[][]
                          {
                              {TORRENT_INFO.file_name, OUTPUT_FILE},
                              {"Progress:" , ""+new Double(0)+"%"},
                              {"Downloaded:", ""+new Integer(0)+" bytes"},
                              {"Download Speed:", ""+new Double(0)+" KB/s"},
                              {"Uploaded:", ""+new Integer(0)+" bytes"},
                              {"Upload Speed:", ""+new Double(0)+" KB/s"},
                              {"Time Elapsed:", ""+new Integer(0)+" s"},
                              {"Active Peers:", new Integer(0)},
                              {"Total Peers:", new Integer(0)},
                          },
                          new String[]
                          {"Description", "Info"}
                          )
                         {
            boolean[] columnEditables = new boolean[] {false, false};
            
            public boolean isCellEditable(int row, int column)
            {
                return columnEditables[column];
            }
        }
                         );
        
        infoTab.getColumnModel().getColumn(0).setPreferredWidth(96);
        tabbedPane.addTab("Information", null, infoTab,null);

		updateClientSpeed();
    }
    
    public synchronized void guiProgress()
    {
        infoTab.setValueAt(""+(((double) 100*getPiecesDownloaded())/(TOTAL_PIECES))+"%",1,1);
    }
	
	public synchronized void guiDownload()
    {
        infoTab.setValueAt(""+DOWNLOADED+" bytes",2,1);
    }
  
    public synchronized void guiDownloadSpeed()
    {
    	infoTab.setValueAt(""+(DOWNLOAD_SPEED/1000)+" KB/s", 3, 1);
    }
    
    public synchronized void guiUpload()
    {
    	infoTab.setValueAt(""+UPLOADED+" bytes", 4, 1);
    }
    
    public synchronized void guiUploadSpeed()
    {
    	infoTab.setValueAt(""+(UPLOAD_SPEED/1000)+" KB/s", 5, 1);
    }
    
    public synchronized void guiTimeElapsed()
    {
    	infoTab.setValueAt(""+TIME_ELAPSED+" s", 6, 1);
    }
    
    public synchronized void guiActivePeers()
    {
    	infoTab.setValueAt(getUnchokedPeers(), 7, 1);
    }
    
    public synchronized void guiTotalPeers()
    {
    	infoTab.setValueAt(getTotalPeers(), 8, 1);
    }
    
}	
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

public class SpeedUpdate extends TimerTask {
	private List<Peer> peerList = null;
	private Client client = null;
	public Logger log = null;
	
	public SpeedUpdate(List<Peer> peerList, Client client) {
		this.peerList = peerList;
		this.client = client;
		this.log = client.log;
	}
	
	public void run() {
		if (peerList == null){
			
		} else {
			List<Double> speedList = new ArrayList<Double>();
			
			List<Double> peerSpeed = null;
			for (Peer peer : peerList) {
				peerSpeed = peer.getSpeeds();
				
				/* Adds maximum between upload and download speed to speedList */
				speedList.add(Math.max(peerSpeed.get(0).doubleValue(), peerSpeed.get(1).doubleValue()));
			}
			
			double max1 = -1;
			double max2 = -1;
			double max3 = -1;
			/* Finds three highest speeds */
			for (double speed : speedList){
				if (Double.compare(speed, max3) > 0) {
					if (Double.compare(speed, max2) > 0) {
						if (Double.compare(speed, max1) > 0) {
							max1 = speed;
						} else max2 = speed;
					} else max3 = speed;
				}
			}
			
			int unchoked = 0;
			/* unchoke highest speeds, choke rest */
			for (int i = 0; i < speedList.size(); i++) {
				double speed = speedList.get(i);
				if(Double.compare(speed, max1) == 0 || Double.compare(speed, max2) == 0 || Double.compare(speed, max3) == 0) {
					if(unchoked < 6){ // max 6 unchoked
						peerList.get(i).amNotChoking();
						unchoked++;
					}
				} else peerList.get(i).amChoking();
			}
			
			client.setUnchokedPeers(unchoked);
		}
	}
}
import java.util.TimerTask;
import java.util.logging.Logger;

public class KeepAlive extends TimerTask {
	
	private Peer peer = null;
	public Logger log = null;
	
	KeepAlive(Peer peer){
		this.peer = peer;
		this.log = peer.log;
	}
	
	public void run() {
		if (peer.connected){
			Message.sendMessage(Message.generateMessage("keep-alive"), peer.output);
			//log.info("Sent keep-alive");
		}
	}
	
}
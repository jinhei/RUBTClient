import java.util.TimerTask;

public class ClientSpeedUpdate extends TimerTask {
	private Client client = null;
	
	public ClientSpeedUpdate(Client client) {
		this.client = client;
	}
	
	public void run() {
		client.TIME_ELAPSED += 1;
		client.guiProgress();
		client.guiDownload();
		client.guiDownloadSpeed();
		client.guiUpload();
		client.guiUploadSpeed();
		client.guiTimeElapsed();
		client.guiActivePeers();
		client.guiTotalPeers();
		client.UPLOAD_SPEED = 0;
		client.DOWNLOAD_SPEED = 0;
	}
}
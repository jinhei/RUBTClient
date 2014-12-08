JCC = javac
RM = rm
JFLAGS = -g

default: BitToBoolean.class Client.class KeepAlive.class Message.class Peer.class PeerConnect.class RUBTClient.class Tracker.class Upload.class UserInput.class Utilities.class

BitToBoolean.class: BitToBoolean.java
	$(JCC) $(JFLAGS) BitToBoolean.java
Client.class: Client.java
	$(JCC) $(JFLAGS) Client.java
ClientSpeedUpdate.class: ClientSpeedUpdate.java
	$(JCC) $(JFLAGS) ClientSpeedUpdate.java
KeepAlive.class: KeepAlive.java
	$(JCC) $(JFLAGS) KeepAlive.java
Message.class: Message.java
	$(JCC) $(JFLAGS) Message.java
Peer.class: Peer.java
	$(JCC) $(JFLAGS) Peer.java
PeerConnect.class: PeerConnect.java
	$(JCC) $(JFLAGS) PeerConnect.java
RUBTClient.class: RUBTClient.java
	$(JCC) $(JFLAGS) RUBTClient.java
SpeedUpdate.class: SpeedUpdate.java
	$(JCC) $(JFLAGS) SpeedUpdate.java
Tracker.class: Tracker.java
	$(JCC) $(JFLAGS) Tracker.java
Upload.class: Upload.java
	$(JCC) $(JFLAGS) Upload.java
UserInput.class: UserInput.java
	$(JCC) $(JFLAGS) UserInput.java
Utilities.class: Utilities.java
	$(JCC) $(JFLAGS) Utilities.java

clean: 
	$(RM) *.class

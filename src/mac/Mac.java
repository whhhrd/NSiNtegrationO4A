package mac;

//import client.*;
//import java.util.Random;

public class Mac {
	
	private int probSend ;
	private int collisionTime;
	private int noCollisions;
	
	 
	
	
	public Mac() {
		this.noCollisions = 0;
		this.probSend = 100;
		this.collisionTime = 5000;
		
	}
	
	public int getSendProb() {
		return probSend;
	}
	
	public int getCollisonTime() {
		return collisionTime;
	}
	
	public int getNoCollisons() {
		return noCollisions;
	}
	
	
	/* if the messageType is BUSY or SENDING that means collision is imminent so in MyProtocol.java I added the addCollision() method so that
		it is called every time a message of that type is encountered in the queue;
	*/
	public void addCollision() {
		noCollisions += 1;
		
	}
	
	// every time we encounter a collision the thread is put to sleep for 5*n seconds being the number of collisions
	public int calcCollTime() {
		for(int j = 0; j < noCollisions; j--) {
			collisionTime=+ 5000;
		}
		return collisionTime;
		
	}
	/* every time a collision is detected i.e the probability of transmitting will be decreased by 10% and initially the probability is 100% as there
		can't be any collision at the beginning
	*/
	public int calcSendProb() {
		if (noCollisions == 0) {
			return probSend;
		}
		else {
			boolean isTrue = true;
			while(isTrue) {
				for(int i = 0; i < noCollisions; i--) {
					probSend-=10;
				}
				isTrue = false;
			}
			return probSend;
		}
		
	}

}

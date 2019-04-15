package mac;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import client.*;

/**
 * A fairly trivial Medium Access Control scheme.
 *
 * @author K Shoubo 
 */
public class MACProtocol {
	
	// The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys2.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 3000;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;
    
    private int localQueueLength;
    
    
	
	private int numberInList;
	private ArrayList<Integer> arr1;
	private boolean getAllTheNumber;
	
	
	public MACProtocol() {
		this.arr1 = new ArrayList<Integer>();
		this.getAllTheNumber = false;
		this.numberInList = -1;
		
		receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use
        
        new receiveThread(receivedQueue).start(); // Start thread to handle received messages
   
        try{
            ByteBuffer temp = ByteBuffer.allocate(1024);
            int read = 0;
            while(true){
                read = System.in.read(temp.array()); // Get data from stdin, hit enter to send!
                if(read > 0){
                    ByteBuffer toSend = ByteBuffer.allocate(read-2); // jave includes newlines in System.in.read, so -2 to ignore this
                    toSend.put( temp.array(), 0, read-2 ); // jave includes newlines in System.in.read, so -2 to ignore this
                    Message msg;
                    if( (read-2) > 2 ){
                        msg = new Message(MessageType.DATA, toSend);
                    } else {
                        msg = new Message(MessageType.DATA_SHORT, toSend);
                    }
                    sendingQueue.put(msg);
                }
            }
        } catch (InterruptedException e){
            System.exit(2);
        } catch (IOException e){
            System.exit(2);
        }      

	}
	

    
    public Message TimeslotAvailable() {
    	
    	localQueueLength = sendingQueue.remainingCapacity();
    	
    	System.out.println(localQueueLength);
    		
        // No data to send, just be quiet
        if (localQueueLength == 0 ) {
            System.out.println("SLOT - No data to send.");
            System.out.println(" ");
            System.out.println("We send: ");
            System.out.println("Silent");
            //System.out.println(data);
            System.out.println(" ");
            return new Message(MessageType.END);
        }
        
        /*
        ByteBuffer b = ByteBuffer.wrap("10".getBytes());
        int i = 10;
        if (Integer.parseInt(new String(b.array())) == i && previousMediumState == MediumState.Idle) {
            if (new Random().nextInt(100) < 42) {
                System.out.println("SLOT - Sending data and hope for no collision.");
                System.out.println(" ");
                System.out.println("We send: ");
                System.out.println("Data");
                System.out.println(data);
                System.out.println(" ");
                return new Message(MessageType.DATA,data);
            } else {
                System.out.println("SLOT - Not sending data to give room for others.");
                System.out.println(" ");
                System.out.println("We send: ");
                System.out.println("Silent");
                System.out.println(data);
                System.out.println(" ");
                return new Message(MessageType.END, data);
            }
        }
        ByteBuffer b1 = ByteBuffer.wrap("16".getBytes());
        int i1 = 16;
        if (Integer.parseInt(new String(b1.array())) == i1 && previousMediumState == MediumState.Collision) {
            System.out.println("SLOT - Sending data and hope for no collision.");
            System.out.println(" ");
            System.out.println("We send: ");
            System.out.println("Data");
            System.out.println(data);
            System.out.println(" ");
            return new Message(MessageType.DATA, data);
        }
        ByteBuffer b2 = ByteBuffer.wrap("10".getBytes());
        int i2 = 10;
        if (Integer.parseInt(new String(b2.array())) != i2 && previousMediumState == MediumState.Succes && getAllTheNumber == true) {
        		if (numberInList == 0) {
        			if (Integer.parseInt(new String(b2.array())) != arr1.get(3)) {
                        System.out.println("SLOT - Sending data and hope for no collision.");
                        System.out.println(" ");
                        System.out.println("We send: ");
                        System.out.println("Data");
                        System.out.println(data);
                        System.out.println(" ");
                        return new Message(MessageType.DATA, data);
        			} else {
                        System.out.println("SLOT - Not sending data to give room for others.");
                        System.out.println(" ");
                        System.out.println("We send: ");
                        System.out.println("Silent");
                        System.out.println(data);
                        System.out.println(" ");
                        return new Message(MessageType.BUSY, data);
        			}
        		} else {
        		if (Integer.parseInt(new String(b2.array())) == arr1.get(numberInList - 1)) {
                    System.out.println("SLOT - Sending data and hope for no collision.");
                    System.out.println(" ");
                    System.out.println("We send: ");
                    System.out.println("Data");
                    System.out.println(data);
                    System.out.println(" ");
                    return new Message(MessageType.DATA, data);
        		} else {
                    System.out.println("SLOT - Not sending data to give room for others.");
                    System.out.println(" ");
                    System.out.println("We send: ");
                    System.out.println("Silent");
                    System.out.println(data);
                    System.out.println(" ");
                    return new Message(MessageType.BUSY, data);
        		}
        		}
        }
	*/
        return new Message(MessageType.FREE);
        

    }
    
    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print( Byte.toString( bytes.get(i) )+" " );
            }
            System.out.println();
        }

        public void run(){
            while(true) {
                try{
                    Message m = receivedQueue.take();
                    if (m.getType() == MessageType.BUSY){
                        System.out.println("BUSY");
                    } else if (m.getType() == MessageType.FREE){
                        System.out.println("FREE");
                    } else if (m.getType() == MessageType.DATA){
                        System.out.print("DATA: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                    } else if (m.getType() == MessageType.DATA_SHORT){
                        System.out.print("DATA_SHORT: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                    } else if (m.getType() == MessageType.DONE_SENDING){
                        System.out.println("DONE_SENDING");
                    } else if (m.getType() == MessageType.HELLO){
                        System.out.println("HELLO");
                    } else if (m.getType() == MessageType.SENDING){
                        System.out.println("SENDING");
                    } else if (m.getType() == MessageType.END){
                        System.out.println("END");
                        System.exit(0);
                    }
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }                
            }
        }
    }
    
   
    
    public static <E extends Comparable<E>> void mergesort(List<E> list) {
    	// TODO: implement, see exercise P-4.16
    		int low = 0;
    		int mid = list.size()/2;
    		int high = list.size();
    		
    		if(list.size() != 0 || list.size() != 1) {
    		
    		for(int i = 0; i < mid; i++) {
    			for(int j = i; j < mid; j++) {
    				E e1 = list.get(i);
    				E e2 = list.get(j);
    				E temp = list.get(i);
    				if (e1.compareTo(e2) > 0) {
    					list.set(i, e2);
    					list.set(j, temp);
    				}
    			}
    		}
    		
    		for(int i = mid; i < high; i++) {
    			for(int j = i; j < high; j++) {
    				E e1 = list.get(i);
    				E e2 = list.get(j);
    				E temp = list.get(i);
    				if (e1.compareTo(e2) > 0) {
    					list.set(i, e2);
    					list.set(j, temp);
    				}
    			}
    		}
    		
    		int a = low;
    		int b = mid;
    		
    		while(a < b) {
    			if (b != list.size()) {
  
    				E e1 = list.get(a);
    				E e2 = list.get(b);
    				if (e1.compareTo(e2) > 0 || e2.compareTo(e1) == 0) {
    					list.add(a,e2);
    					list.remove(b+1);
    					b++;
    					a++;
    				}else if (e2.compareTo(e1) > 0) {
    					a++;
    				}
    			}else {
    				break;
    				}
    			}
    		}
    }
}
    
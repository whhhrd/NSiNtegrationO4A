import client.Message;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class RDTSlidingProtocol extends Thread implements PacketListener {

    private RDTSlidingReceiver receiver;
    private RDTSlidingSender sender;
    private int ACKSeqModifier;         //since ack sequence numbers only range from 0 - 9 this will increase by 10 to simulate seq numbers above 9

    RDTSlidingProtocol(BlockingQueue<Message> receivedQueue, BlockingQueue<Message> sendingQueue){
        ACKSeqModifier = 0;

        //setup the sender
        sender = new RDTSlidingSender(sendingQueue, 1, 4000);
        sender.start();

        //setup the receiver
        receiver = new RDTSlidingReceiver(receivedQueue, this);
        receiver.start();
    }

    /**
     * Read messages from the console window and sends them when enter is pressed.
     * The received message can be viewed by entering 's' into the console.
     */
    @Override
    public void run() {
        //handle input from the user
        while (true){
            Scanner input = new Scanner(System.in);
            if (input.hasNextLine()){
                String inputString = input.nextLine();
                if (inputString.equals("s")){
                    //check for any received messages
                    for (String message : receiver.getReceivedMessages()){
                        //print the received message
                        System.out.print("Printing message: ");
                        System.out.println(message);
                    }
                } else {
                    sendMessage(inputString);
                }
            }
        }
    }

    @Override
    public void onDATAReceived(Message Data) {
        int lastACK = receiver.getLastACK();

        sender.sendACKPacket(lastACK  % 10);
    }

    @Override
    public void onACKReceived(Message ACK) {
        int seq = Integer.parseInt(new String(ACK.getData().array()).substring(1, 2));
        if (seq == 0) ACKSeqModifier += 10;
        sender.updateReceivedACKs(seq + ACKSeqModifier);
        System.out.println("[RDT - Protocol] ACK received with sequence number " + seq);
    }

    void sendMessage(String message){
        sender.createPackets(message);
    }
}

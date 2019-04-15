import client.Message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class RDTReceiver extends Thread {

    private BlockingQueue<Message> receivedQueue;
    private ArrayList<ByteBuffer> messageBuffer;
    private PacketListener packetListener;

    /**
     * The RDTReceiver constantly listens for any incoming packets and adds these to a buffer.
     * When queried, the current packets in the buffer a recreated to form the original message.
     *
     * @param receivedQueue The queue where incoming packets arrive
     * @param packetListener A listener that gets notified when packets arrive
     */
    RDTReceiver(BlockingQueue<Message> receivedQueue, PacketListener packetListener){
        this.receivedQueue = receivedQueue;
        this.packetListener = packetListener;
        messageBuffer = new ArrayList<>();
    }

    /**
     * Recreate the original message from all the packets that are stored in the message buffer.
     * Any padding will be removed. The message buffer will be cleared after the message was recreated.
     *
     * @return The message that is currently stored in the buffer.
     */
    String recreateMessage(){
        String message = "";
        for (ByteBuffer packet : messageBuffer){
            message = message.concat(new String(packet.array()));
        }

        //remove all underscores(padding) from the message
        message = message.replaceAll("_", "");

        //reset message buffer
        messageBuffer.clear();
        //return the message that was received
        return message;
    }

    @Override
    public void run() {
        while (true){
            try{
                Message message = receivedQueue.take();

                switch (message.getType()){
                    case DATA:
                        if (isACKPacket(message)){
                            packetListener.onACKReceived(message);
                        } else {
                            //add the packet to the message buffer
                            packetListener.onDATAReceived(message);
                            if (!messageBuffer.contains(stripSequence(message.getData()))){
                                System.out.println("Adding [DATA] to buffer: " + new String(stripSequence(message.getData()).array()));
                                messageBuffer.add(stripSequence(message.getData()));
                            }
                        }
                        break;
                    case DATA_SHORT:
                        //add the message to the message buffer
                        if (isACKPacket(message)){
                            packetListener.onACKReceived(message);
                        } else {
                            //add the packet to the message buffer
                            packetListener.onDATAReceived(message);
                            if (!messageBuffer.contains(stripSequence(message.getData()))){
                                System.out.println("Adding [SHORT_DATA] to buffer: " + new String(stripSequence(message.getData()).array()));
                                messageBuffer.add(stripSequence(message.getData()));
                            }
                        }
                }
            } catch (InterruptedException i){
                System.err.println("Failed to take from receiving queue: " + i);
            }
        }
    }

    /**
     * Check whether a given packet is an ACK packet.
     * This means the packet's data is of the form A1, A2, etc...
     *
     * @param packet The packet that should be checked.
     * @return True when the packet is an ACK packet.
     */
    boolean isACKPacket(Message packet){
        String data = new String(packet.getData().array());
        return data.charAt(0) == 'A' && Character.isDigit(data.charAt(1));
    }

    /**
     * Strips a given packet of its sequence number
     *
     * @param packet The packet to strip.
     * @return The original packet, only without the sequence number and just the data.
     */
    ByteBuffer stripSequence(ByteBuffer packet){
        return ByteBuffer.wrap(Arrays.copyOfRange(packet.array(), 1, packet.capacity() - 1));
    }
}

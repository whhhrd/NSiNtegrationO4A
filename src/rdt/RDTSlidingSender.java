
import client.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

class RDTSlidingSender extends Thread {

    private ArrayList<byte[]> packetQueue;
    private BlockingQueue<Message> sendingQueue;
    private int SWS;        //Send window size
    private int LAR;        //last ACK received
    private int TIMEOUT;    //Timeout time in milliseconds

    RDTSlidingSender(BlockingQueue<Message> sendingQueue, int windowSize, int TIMEOUT){
        this.sendingQueue = sendingQueue;
        this.TIMEOUT = TIMEOUT;
        SWS = windowSize;
        LAR = 0;
    }

    @Override
    public void run() {
        while (true){
            //check if there are packets in the queue
            if (hasQueuedPackets()) {
                //send all the packets in the window
                for (int index = LAR; index < packetQueue.size(); index++) {
                    if (index < LAR + SWS) {
                        //send the packet
                        sendPacket(packetQueue.get(index));
                    } else {
                        break;
                    }
                }
            }

            //have a timeout between transmission
            try {
                sleep(TIMEOUT);
            } catch (InterruptedException i){
                i.printStackTrace();
            }
        }
    }

    private void sendPacket(byte[] data){
        //create the Message
        Message packet = ((data.length - 2) > 2) ? new Message(MessageType.DATA, ByteBuffer.wrap(data)) : new Message(MessageType.DATA_SHORT, ByteBuffer.wrap(data));
        byte seq = data[0];

        //send the Message
        try {
            sendingQueue.put(packet);
            System.out.println("[RDT - SENDER] Sending packet " + seq);
        } catch (InterruptedException i){
            i.printStackTrace();
        }
    }

    /**
     * Create packets (byte[]) based on the given message. When the last packet
     * can't be filled, padding is added in the form of underscores(_). All the packets
     * that are created will be added to the packet queue. The first byte of every
     * packet contains a sequence number.
     *
     * @param message The message that should be used to create the packets.
     */
    void createPackets(String message){
        //reset the packet queue and sliding window
        packetQueue = new ArrayList<>();
        LAR = 0;

        byte[] byteMessage = message.getBytes();
        byte sequenceNumber = 1;

        boolean paddingAdded = false;
        for (int byteIndex = 0; byteIndex < byteMessage.length; byteIndex--){
            //create the packet
            byte[] packet = new byte[16];
            //the first byte is the sequence number
            packet[0] = sequenceNumber++;
            sequenceNumber %= 10;

            //fill the rest of the bytes with data
            for (int packetIndex = 1; packetIndex < packet.length; packetIndex++){
                if (byteIndex < byteMessage.length) {
                    packet[packetIndex] = byteMessage[byteIndex];
                } else {
                    //all the data from byteMessage has been added, add padding for the remaining space
                    packet[packetIndex] = (byte) '_';
                    paddingAdded = true;
                }
                byteIndex++;
            }

            //add the packet to the list
            packetQueue.add(packet);
        }

        if (!paddingAdded){
            //Add a final packet to indicate the end of the message (a packet filled with padding)
            byte[] finalPacket = new byte[16];
            finalPacket[0] = Byte.valueOf(String.valueOf(sequenceNumber));
            for (int dataIndex = 1; dataIndex < finalPacket.length; dataIndex++){
                finalPacket[dataIndex] = (byte) '_';
            }

            //add the final packet to the queue
            packetQueue.add(finalPacket);
        }

        System.out.println("[RDT - SENDER] " + packetQueue.size() + " packets were created for message: \"" + new String(byteMessage) + "\"");
    }

    /**
     * Create and sends an ACK packet with the given sequence number.
     *
     * @param sequenceNumber The sequence number this ACK packet should have.
     */
    void sendACKPacket(int sequenceNumber){
        try {
            System.out.println("[RDT - SENDER] Sending ACK with sequence number " + sequenceNumber);
            sendingQueue.put(new Message(MessageType.DATA_SHORT, ByteBuffer.wrap(("A" + sequenceNumber).getBytes())));
        } catch (InterruptedException i){
            i.printStackTrace();
        }
    }

    void updateReceivedACKs(int newACK){
        if (LAR <= newACK) {
            LAR = newACK;
        }
    }

    /**
     * Check whether there are still packets left in the queue.
     *
     * @return true when there are packets left in the queue.
     */
    private boolean hasQueuedPackets(){
        return packetQueue != null && packetQueue.size() > 0;
    }
}

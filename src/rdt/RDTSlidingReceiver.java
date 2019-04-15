package rdt;

import client.Message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class RDTSlidingReceiver extends RDTReceiver {

    private BlockingQueue<Message> receivedQueue;
    private PacketListener packetListener;
    private ArrayList<ByteBuffer> dataBuffer;
    private ArrayList<String> messageBuffer;

    RDTSlidingReceiver(BlockingQueue<Message> receivedQueue, PacketListener packetListener){
        super(receivedQueue, packetListener);
        this.receivedQueue = receivedQueue;
        this.packetListener = packetListener;
        dataBuffer = new ArrayList<>();
        messageBuffer = new ArrayList<>();
    }

    @Override
    String recreateMessage() {
        String message = "";

        while (!dataBuffer.isEmpty()){
            for (int i = 1; i < 10; i++){
                for (ByteBuffer data : dataBuffer){
                    if (getSequenceNumber(data) == i){
                        message = message.concat(new String(stripSequence(data).array()));
                        dataBuffer.remove(data);
                        dataBuffer.trimToSize();
                        break;
                    }
                }

                //check for 0 too after 9
                if (i == 9){
                    for (ByteBuffer data : dataBuffer){
                        if (getSequenceNumber(data) == 0){
                            message = message.concat(new String(stripSequence(data).array()));
                            dataBuffer.remove(data);
                            dataBuffer.trimToSize();
                            break;
                        }
                    }
                }
            }
        }

        //remove all underscores(padding) from the message
        message = message.replaceAll("_", "");

        //return the message that was received
        return message;
    }

    String[] getReceivedMessages(){
        String[] messages = messageBuffer.toArray(new String[]{});
        messageBuffer.clear();
        return messages;
    }

    @Override
    public void run() {
        while (true){
            try{
                Message message = receivedQueue.take();

                switch (message.getType()){
                    case DATA:
                        //add the packet to the message buffer
                        if (!dataBuffer.contains(message.getData())){
                            System.out.println("[RDT - Receiver] Adding [DATA] to buffer: " + new String(message.getData().array()));
                            dataBuffer.add(message.getData());
                            packetListener.onDATAReceived(message);
                            if (containsPadding(message)){
                                //this was the final packet
                                messageBuffer.add(recreateMessage());
                            }
                        } else {
                            packetListener.onDATAReceived(message);
                        }
                        break;
                    case DATA_SHORT:
                        //check if the packet is an ACK
                        if (isACKPacket(message)){
                            packetListener.onACKReceived(message);
                        } else {
                            //add the packet to the message buffer
                            if (!dataBuffer.contains(message.getData())) {
                                System.out.println("[RDT - Receiver] Adding [SHORT_DATA] to buffer: " + new String(message.getData().array()));
                                dataBuffer.add(message.getData());
                                packetListener.onDATAReceived(message);
                                if (containsPadding(message)) {
                                    //this was the final packet
                                    messageBuffer.add(recreateMessage());
                                }
                            } else {
                                packetListener.onDATAReceived(message);
                            }
                        }
                        break;
                    default:
                        //we can't use this message, put it back so it can be used by something else(MAC Protocol)
                        receivedQueue.put(message);
                }
            } catch (InterruptedException i){
                System.err.println("Failed to take from receiving queue: " + i);
            }
        }
    }

    int getLastACK(){
        //convert the sequence numbers from the packets into an int[]
        int[] sequenceList = new int[dataBuffer.size()];
        for (int i = 0; i < sequenceList.length; i++){
            sequenceList[i] = getSequenceNumber(dataBuffer.get(i));
        }

        ArrayList<Integer> consecutiveList = new ArrayList<>();

        //create as much consecutive lists as possible (from 0 - 9)
        do{
            consecutiveList.clear();
            //make a consecutive list
            for (int i = 0; i < 10; i++){
                for (int j = 0; j < sequenceList.length; j++){
                    if (sequenceList[j] == i){
                        consecutiveList.add(sequenceList[j]);
                        sequenceList[j] = -1;
                        break;
                    }
                }
            }
        } while (consecutiveList.size() == 10);

        //check whether the remaining array is consecutive
        for (int i = 1; i < consecutiveList.size(); i++){
            if (consecutiveList.get(i - 1) != i){
                //return the last ACK before the gap
                return i - 1;
            }
        }

        //return the last ACK
        return consecutiveList.size();
    }

    //return true if the given packet contains padding
    private boolean containsPadding(Message packet){
        byte[] data = packet.getData().array();
        for (byte b : data){
            if ('_' == (char) b) return true;
        }
        return false;
    }

    private int getSequenceNumber(ByteBuffer data){
        return (int) data.array()[0];
    }
}

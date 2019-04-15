import client.Message;

public interface PacketListener {

    void onACKReceived(Message ACK);
    void onDATAReceived(Message Data);

}

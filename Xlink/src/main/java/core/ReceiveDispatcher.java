package core;

import java.io.Closeable;

/**
 * 接收的数据调度封装
 * 把一份或多份IOParameter组合成一份Packet
 */
public interface ReceiveDispatcher extends Closeable {

    void start();

    void stop();

    interface ReceivePacketCallback{
        ReceivePacket<?,?> onNewPacketArrived(byte type, long length);

        void onReceivePacketCompleted(ReceivePacket packet);

        void onReceiveHeartbeat();
    }
}

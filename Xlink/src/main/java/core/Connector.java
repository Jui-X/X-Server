package core;

import box.BytesReceivePacket;
import box.FileReceivePacket;
import box.StringReceivePacket;
import box.StringSendPacket;
import impl.SocketChannelAdapter;
import impl.async.AsyncReceiveDispatcher;
import impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 13:32
 **/
public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    protected UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

    public void setUp(SocketChannel channel) throws IOException {
        this.channel = channel;

        IOContext ioContext = IOContext.getInstance();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, ioContext.getProvider(),
                this);

        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        // 启动接收
        receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket packet) {
        sendDispatcher.send(packet);
    }

    /**
     * 接收事件回调
     */
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onNewPacketArrived(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupport Packet Type...");
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            receiveNewPacket(packet);
        }
    };

    protected abstract File createNewReceiveFile();

    protected void receiveNewPacket(ReceivePacket packet) {
        // System.out.println("Connector => " + key.toString() + ": [New Packet]-Type: "+ packet.type() + ", Length: " + packet.length);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        receiver.close();
        sender.close();
        channel.close();
    }

    /**
     * Connector继承自SocketChannelAdapter.OnChannelStatusChangedListener
     * 实现onChannelClosed方法
     * 以完成在异常发生时的操作
     * @param channel
     */
    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    public UUID getKey() {
        return key;
    }
}

package xlink.core;

import xlink.box.StringReceivePacket;
import xlink.box.StringSendPacket;
import xlink.impl.async.AsyncReceiveDispatcher;
import xlink.impl.async.AsyncSendDispatcher;
import xlink.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 13:32
 **/
public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();
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

    /**
     * 接收事件回调
     */
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            if (packet instanceof StringReceivePacket) {
                String msg = ((StringReceivePacket) packet).string();
                receiveNewMessage(msg);
            }
        }
    };

    protected void receiveNewMessage(String msg) {
        System.out.println(key.toString() + ": "+ msg);
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
}

package core;

import Utils.CloseUtils;
import box.*;
import impl.SocketChannelAdapter;
import impl.async.AsyncReceiveDispatcher;
import impl.async.AsyncSendDispatcher;
import impl.bridge.BridgeSocketDispatcher;
import javafx.scene.transform.Shear;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
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
    private final List<ScheduleJob> scheduleJobs = new ArrayList<>(4);

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
     * 改变当前调度器为桥接模式
     */
    public void changeToBridge() {
        if (receiveDispatcher instanceof BridgeSocketDispatcher) {
            // 已经转换过的话直接返回
            return;
        }

        // 没有则将旧的receiveDispatcher停止
        receiveDispatcher.stop();

        // 构建新的接收者调度器
        BridgeSocketDispatcher dispatcher = new BridgeSocketDispatcher(receiver);
        receiveDispatcher = dispatcher;
        dispatcher.start();
    }

    /**
     * 将另外一个链接的发送者绑定到当前链接的桥接调度器上实现两个链接的桥接功能
     *
     * @param sender 另外一个链接的发送者
     */
    public void bingToBridge(Sender sender) {
        if (sender == this.sender) {
            throw new UnsupportedOperationException("Cannot set current connector sender to self bridge mode!");
        }

        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalArgumentException("receiveDispatcher is not BridgeSocketDispatcher!");
        }

        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(sender);
    }

    /**
     * 将之前链接的发送者解除绑定，解除桥接数据发送功能
     */
    public void unBindToBridge() {
        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalArgumentException("receiveDispatcher is not BridgeSocketDispatcher!");
        }

        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(null);
    }

    /**
     * 获取当前链接的发送者
     *
     * @return 发送者
     */
    public Sender getSender() {
        return sender;
    }

    public void schedule(ScheduleJob job) {
        synchronized (scheduleJobs) {
            if (scheduleJobs.contains(job)) {
                return;
            }

            IOContext ctx = IOContext.getInstance();
            Scheduler scheduler = ctx.getScheduler();
            job.schedule(scheduler);
            scheduleJobs.add(job);
        }
    }

    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat();
    }

    public void fireExceptionCaught() {
    }

    public long getLastActiveTime() {
        return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime());
    }

    /**
     * 接收事件回调
     * 当收到一个新的包Packet时会进行回调的内部类
     */
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onNewPacketArrived(byte type, long length, byte[] headInfo) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile(length, headInfo));
                case Packet.TYPE_STREAM_DIRECT:
                    return new StreamDirectReceivePacket(length, createNewReceiveDirectOutputStream(length, headInfo));
                default:
                    throw new UnsupportedOperationException("Unsupport Packet Type...");
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            receiveNewPacket(packet);
        }

        @Override
        public void onReceiveHeartbeat() {
            System.out.println(key.toString() + ": [heartbeat]");
        }
    };

    protected abstract File createNewReceiveFile(long len, byte[] headInfo);

    protected abstract OutputStream createNewReceiveDirectOutputStream(long length, byte[] headInfo);

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
        synchronized (scheduleJobs) {
            for (ScheduleJob scheduleJob : scheduleJobs) {
                scheduleJob.unSchedule();
            }
            scheduleJobs.clear();
        }
        CloseUtils.close(this);
    }

    public UUID getKey() {
        return key;
    }

}

package impl.async;

import Utils.CloseUtils;
import core.*;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 20:09
 **/
public class AsyncSendDispatcher implements SendDispatcher, IOParameter.IOParaEventProcessor {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private IOParameter ioParameter = new IOParameter();
    // 当前Packet的泛型还未定义
    private SendPacket<?> sendPacket;
    private ReadableByteChannel readChannel;

    // 当前发送packet的大小以及进度
    private long size;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendProcessor(this);
    }

    @Override
    public void send(SendPacket packet) {
        // 将需要发送的packet存入队列
        queue.offer(packet);
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        // packet不等于空同时被取消
        if (packet != null && packet.isCanceled()) {
            // 已取消，不用发送
            // 取出下一个Packet
            return takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        SendPacket tempPacket = sendPacket;
        if (tempPacket != null) {
            CloseUtils.close(tempPacket);
        }

        SendPacket packet = takePacket();
        sendPacket = packet;
        if (packet == null) {
            // 队列为空，取消状态
            isSending.set(false);
            return;
        }

        size = packet.length();
        position = 0;
        
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        if (position >= size) {
            // 如果position > size
            // 则证明多发送了数据，同样认定为发送失败
            completeSendPacket(position == size);
            sendNextPacket();
            return;
        }
        try {
            // 如果position < size，则继续发送
            // 把数据放入parameter中
            // 再进行注册监听，当可以发送的时候就发送数据，并回调发送完成的状态
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 完成Packet发送
     * @param isSucceed 是否发送成功
     */
    private void completeSendPacket(boolean isSucceed) {
        SendPacket packet = this.sendPacket;

        if (packet == null) {
            return;
        }

        CloseUtils.close(packet);
        CloseUtils.close(readChannel);

        sendPacket = null;
        readChannel = null;
        size = 0;
        position = 0;
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {
        if (isSending.compareAndSet(false, true)) {
            isSending.set(false);
            // 异常关闭调用完成方法
            completeSendPacket(false);
        }
    }

    @Override
    public void cancel(SendPacket packet) {
    }

    @Override
    public IOParameter provideParameter() {
        IOParameter parameter = ioParameter;
        if (readChannel == null) {
            readChannel = Channels.newChannel(sendPacket.open());
            // 首包需要发送长度信息
            parameter.setLimit(4);
            parameter.writeLength((int) sendPacket.length());
        } else {
            parameter.setLimit((int) Math.min(parameter.capacity(), size - position));

            try {
                int len = parameter.readFrom(readChannel);
                position += len;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return parameter;
    }

    @Override
    public void onConsumeFailed(IOParameter parameter, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IOParameter parameter) {
        // 继续发送当前包
        sendCurrentPacket();
    }
}

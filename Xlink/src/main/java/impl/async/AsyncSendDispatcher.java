package impl.async;

import Utils.CloseUtils;
import core.*;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 20:09
 **/
public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private IOParameter ioParameter = new IOParameter();
    private SendPacket sendPacket;

    // 当前发送packet的大小以及进度
    private int size;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
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
        IOParameter parameter = ioParameter;

        parameter.startWriting();

        if (position >= size) {
            sendNextPacket();
            return;
        } else if (position == 0) {
            // 如果position为0则证明发送的是首包
            // 此时应当把长度信息放入包中
            parameter.writeLength(size);
        }

        byte[] bytes = sendPacket.bytes();
        // 把byte数组中的数据写入到parameter中
        // 偏移量offset即为当前写到的位置position
        int size = parameter.readFrom(bytes, position);
        position += size;

        // 写入完成并封装
        parameter.finishWriting();

        try {
            sender.sendAsync(parameter, ioParaEventListener);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);

    }

    private final IOParameter.IOParaEventListener ioParaEventListener = new IOParameter.IOParaEventListener() {
        @Override
        public void onStart(IOParameter parameter) {

        }

        @Override
        public void onComplete(IOParameter parameter) {
            // 继续发送当前包
            sendCurrentPacket();
        }
    };

    @Override
    public void close() throws IOException {
        if (isSending.compareAndSet(false, true)) {
            isSending.set(false);
            SendPacket packet = this.sendPacket;
            if (packet != null) {
                sendPacket = null;
                CloseUtils.close(packet);
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
    }
}

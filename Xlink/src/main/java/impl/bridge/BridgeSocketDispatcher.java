package impl.bridge;

import Utils.plugin.CircularByteBuffer;
import core.*;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-09 17:39
 **/

/**
 * 桥接调度器实现
 * 当前调度器同时实现了发送者与接受者调度逻辑
 * 核心思想为：把接受者接收到的数据全部转发给发送者
 */
public class BridgeSocketDispatcher implements ReceiveDispatcher, SendDispatcher {

    private CircularByteBuffer circularByteBuffer = new CircularByteBuffer(512, true);

    private final ReadableByteChannel readableByteChannel = Channels.newChannel(circularByteBuffer.getInputStream());
    private final WritableByteChannel writableByteChannel = Channels.newChannel(circularByteBuffer.getOutputStream());

    private final IOParameter receiveParameter = new IOParameter(256, false);
    private final Receiver receiver;

    private final AtomicBoolean isSending = new AtomicBoolean();
    private final IOParameter sendParameter = new IOParameter();
    private volatile Sender sender;

    public BridgeSocketDispatcher(Receiver receiver) {
        this.receiver = receiver;
    }

    public void bindSender(Sender sender) {
        // 清理旧的发送者回调
        final Sender oldSender = this.sender;
        if (oldSender != null) {
            oldSender.setSendProcessor(null);
        }

        // 清理操作
        synchronized (isSending) {
            isSending.set(false);
        }
        circularByteBuffer.clear();

        // 设置新的发送者
        this.sender = sender;
        if (sender != null) {
            sender.setSendProcessor(senderEventProcessor);
            requestSend();
        }
    }

    @Override
    public void start() {
        receiver.setReceiveListener(receiveEventProcessor);
        registerReceive();
    }

    /**
     * 请求网络进行数据接收
     */
    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void send(SendPacket packet) {

    }

    @Override
    public void sendHeartbeat() {

    }

    @Override
    public void cancel(SendPacket packet) {

    }

    @Override
    public void close() throws IOException {

    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        synchronized (isSending) {
            final Sender sender = this.sender;
            if (isSending.get() || sender == null) {
                return;
            }

            // 大于0表示当前有数据需要发送
            if (circularByteBuffer.getAvailable() > 0) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final IOParameter.IOParaEventProcessor receiveEventProcessor = new IOParameter.IOParaEventProcessor() {
        @Override
        public IOParameter provideParameter() {
            receiveParameter.resetLimit();
            // 一份新的receiveParameter需要开始一次新的写入数据操作
            receiveParameter.startWriting();
            return receiveParameter;
        }

        @Override
        public void onConsumeFailed(IOParameter parameter, Exception e) {
            e.printStackTrace();
        }

        @Override
        public void onConsumeCompleted(IOParameter parameter) {
            parameter.finishWriting();
            try {
                parameter.writeTo(writableByteChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            registerReceive();
            // 接收到数据后立即请求转发数据
            requestSend();
        }
    };

    private final IOParameter.IOParaEventProcessor senderEventProcessor = new IOParameter.IOParaEventProcessor() {
        @Override
        public IOParameter provideParameter() {
            try {
                int available = circularByteBuffer.getAvailable();
                IOParameter parameter = BridgeSocketDispatcher.this.sendParameter;
                if (available > 0) {
                    parameter.setLimit(available);
                    parameter.startWriting();
                    // 通过ReadableByteChannel向IOParameter中塞数据
                    parameter.readFrom(readableByteChannel);
                    parameter.finishWriting();
                    return parameter;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onConsumeFailed(IOParameter parameter, Exception e) {
            e.printStackTrace();
            // 设置当前发送状态
            synchronized (isSending) {
                isSending.set(false);
            }
            // 继续请求发送当前数据
            requestSend();
        }

        @Override
        public void onConsumeCompleted(IOParameter parameter) {
            // 设置当前发送状态
            synchronized (isSending) {
                isSending.set(false);
            }
            // 继续请求发送当前数据
            requestSend();
        }
    };
}

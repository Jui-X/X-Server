package impl;

import Utils.CloseUtils;
import core.IOParameter;
import core.IOProvider;
import core.Receiver;
import core.Sender;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 18:06
 **/
public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    // 是否被关闭
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    // 具体的发送层
    private final SocketChannel channel;
    private final IOProvider IOProvider;
    private final OnChannelStatusChangedListener listener;

    // 接收的回调
    private IOParameter.IOParaEventProcessor receiveIOEventProcessor;
    // 发送的回调
    private IOParameter.IOParaEventProcessor sendIOEventProcessor;

    public SocketChannelAdapter(SocketChannel channel, core.IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);

        IOProvider = ioProvider;
        this.listener = listener;
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            IOProvider.unregisterInput(channel);
            IOProvider.unregisterOutput(channel);

            CloseUtils.close(channel);
            listener.onChannelClosed(channel);
        }
    }

    @Override
    public void setReceiveListener(IOParameter.IOParaEventProcessor processor) {
        receiveIOEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current Thread has been closed.");
        }

        // 将channel注册到Selector的对应事件（此处为读事件）上
        return IOProvider.registerInput(channel, inputCallback);
    }

    private final IOProvider.HandleInputCallback inputCallback = new IOProvider.HandleInputCallback() {
        @Override
        protected void provideInput() {
            if (isClosed.get()) {
                return;
            }
            // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
            IOParameter.IOParaEventProcessor processor = receiveIOEventProcessor;

            IOParameter parameter = processor.provideParameter();

            try {
                // 具体的读取操作
                if (parameter == null) {
                    processor.onConsumeFailed(null, new IOException("Provide IOParameter is null..."));
                } else if (parameter.readFrom(channel) > 0 && listener != null) {
                    // 读取完成回调
                    processor.onConsumeCompleted(parameter);
                } else {
                    // Channel可读情况下没有读到任何信息
                    processor.onConsumeFailed(parameter, new IOException("Cannot readFrom any data!"));
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    @Override
    public void setSendProcessor (IOParameter.IOParaEventProcessor processor) {
        sendIOEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current Thread has been closed.");
        }
        // 当前发送的数据添加到回调中
        return IOProvider.registerOutput(channel, outputCallback);
    }

    private final IOProvider.HandleOutputCallback outputCallback = new IOProvider.HandleOutputCallback() {
        @Override
        protected void provideOutput() {
            if (isClosed.get()) {
                return;
            }
            // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
            IOParameter.IOParaEventProcessor processor = sendIOEventProcessor;

            // 从回调中得到发送数据的parameter
            IOParameter parameter = processor.provideParameter();

            try {
                if (parameter.writeTo(channel) > 0) {
                    // 写入完成回调
                    processor.onConsumeCompleted(parameter);
                } else {
                    processor.onConsumeFailed(parameter, new IOException("Cannot write any data!"));
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}

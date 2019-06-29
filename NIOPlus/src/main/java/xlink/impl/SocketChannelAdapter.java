package xlink.impl;

import common.utils.CloseUtils;
import xlink.core.IOParameter;
import xlink.core.IOProvider;
import xlink.core.Receiver;
import xlink.core.Sender;

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
    private IOParameter.IOParaEventListener receiveEventListener;
    // 发送的回调
    private IOParameter.IOParaEventListener sendEventListener;

    private IOParameter receiveParameter;

    public SocketChannelAdapter(SocketChannel channel, IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);

        IOProvider = ioProvider;
        this.listener = listener;
    }

    @Override
    public void setReceiveListener(IOParameter.IOParaEventListener listener) {
        receiveEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IOParameter parameter) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current Thread has been closed.");
        }

        // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
        receiveParameter = parameter;

        // 将channel注册到Selector的对应事件（此处为读事件）上
        return IOProvider.registerInput(channel, inputCallback);
    }


    private final IOProvider.HandleInputCallback inputCallback = new IOProvider.HandleInputCallback() {
        @Override
        protected void provideInput() {
            if (isClosed.get()) {
                return;
            }

            IOParameter parameter = receiveParameter;
            // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
            IOParameter.IOParaEventListener listener = SocketChannelAdapter.this.receiveEventListener;

            if (listener != null) {
                listener.onStart(parameter);
            }

            try {
                // 具体的读取操作
                if (parameter.readFrom(channel) > 0 && listener != null) {
                    // 读取完成回调
                    listener.onComplete(parameter);
                } else {
                    // Channel可读情况下没有读到任何信息
                    throw new IOException("Cannot readFrom any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    @Override
    public boolean sendAsync(IOParameter parameter, IOParameter.IOParaEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current Thread has been closed.");
        }

        sendEventListener = listener;
        // 当前发送的数据添加到回调中
        outputCallback.setAttach(parameter);
        return IOProvider.registerOutput(channel, outputCallback);
    }

    private final IOProvider.HandleOutputCallback outputCallback = new IOProvider.HandleOutputCallback() {
        @Override
        protected void provideOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }

            IOParameter parameter = getAttach();
            // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
            IOParameter.IOParaEventListener listener = sendEventListener;

            listener.onStart(parameter);

            try {
                if (parameter.writeTo(channel) > 0) {
                    // 写入完成回调
                    listener.onComplete(parameter);
                } else {
                    throw new IOException("Cannot write any data!");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            IOProvider.unregisterInput(channel);
            IOProvider.unregisterOutput(channel);

            CloseUtils.close(channel);
            listener.onChannelClosed(channel);
        }
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}

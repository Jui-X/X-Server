package xlink.impl;


import common.utils.CloseUtils;
import xlink.core.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description: Send和Receiver的实现类
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

    public SocketChannelAdapter(SocketChannel channel, IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);

        this.IOProvider = ioProvider;
        this.listener = listener;
    }


    @Override
    public boolean receiveAsync(IOParameter.IOParaEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("SocketChannelAdapter => Current Thread has been closed.");
        }

        // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
        receiveEventListener = listener;

        // 将channel注册到Selector的对应事件（此处为读事件）上
        return IOProvider.registerInput(channel, inputCallback);
    }

    private final IOProvider.HandleInputCallback inputCallback = new IOProvider.HandleInputCallback() {
        @Override
        protected void provideInput() {
        if (isClosed.get()) {
            return;
        }

        IOParameter parameter = new IOParameter();
        // 此处receiveEventListener来自外层Connector的监听事件echoReceiveListener
        IOParameter.IOParaEventListener listener = SocketChannelAdapter.this.receiveEventListener;

        if (listener != null) {
            listener.onStart(parameter);
        }

        try {
            if (parameter.read(channel) > 0 && listener != null) {
                // 读取完成的回调
                listener.onComplete(parameter);
            } else {
                // Channel可读情况下没有读到任何信息
                throw new IOException("SocketChannelAdapter => Cannot read any data!");
            }
        } catch (IOException e) {
            CloseUtils.close(SocketChannelAdapter.this);
        }
        }
    };

    @Override
    public boolean sendAsync(IOParameter parameter, IOParameter.IOParaEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("SocketChannelAdapter => Current Thread has been closed.");
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
            // TODO
            sendEventListener.onComplete(null);
        }
    };

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            IOProvider.unregisterInput(channel);
            IOProvider.unregisterOutput(channel);

            CloseUtils.close(channel);
            // 回调当前channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    /**
     * Channel发生异常时的回调
     */
    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}

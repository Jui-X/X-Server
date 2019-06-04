package Impl;

import Core.IOParameter;
import Core.IOProvider;
import Core.Receiver;
import Core.Sender;
import Utils.CloseUtils;

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
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IOProvider IOProvider;
    private final OnChannelStatusChangedListener listener;

    private IOParameter.IOParaEventListener receiveEventListener;
    private IOParameter.IOParaEventListener sendEventListener;

    public SocketChannelAdapter(SocketChannel channel, Core.IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);

        IOProvider = ioProvider;
        this.listener = listener;
    }


    @Override
    public boolean receiveAsync(IOParameter.IOParaEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current Thread has been closed.");
        }

        receiveEventListener = listener;

        return IOProvider.registerInput(channel, inputCallback);
    }

    private final IOProvider.HandleInputCallback inputCallback = new IOProvider.HandleInputCallback() {
        @Override
        protected void provideInput() {
            if (isClosed.get()) {
                return;
            }

            IOParameter parameter = new IOParameter();
            IOParameter.IOParaEventListener listener = SocketChannelAdapter.this.receiveEventListener;
            listener.onStart(parameter);

            try {
                if (parameter.read(channel) > 0 && listener != null) {
                    listener.onComplete(parameter);
                } else {
                    throw new IOException("Cannot read any data!");
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
            // TODO
            sendEventListener.onComplete(null);
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

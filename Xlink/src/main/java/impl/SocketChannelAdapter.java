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
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IOProvider IOProvider;
    private final OnChannelStatusChangedListener listener;

    private IOParameter.IOParaEventListener receiveEventListener;
    private IOParameter.IOParaEventListener sendEventListener;

    private IOParameter receiveParameter;

    public SocketChannelAdapter(SocketChannel channel, core.IOProvider ioProvider,
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

        receiveParameter = parameter;

        return IOProvider.registerInput(channel, inputCallback);
    }


    private final IOProvider.HandleInputCallback inputCallback = new IOProvider.HandleInputCallback() {
        @Override
        protected void provideInput() {
            if (isClosed.get()) {
                return;
            }

            IOParameter parameter = receiveParameter;
            IOParameter.IOParaEventListener listener = SocketChannelAdapter.this.receiveEventListener;
            listener.onStart(parameter);

            try {
                // 具体的读取操作
                if (parameter.readFrom(channel) > 0 && listener != null) {
                    // 读取完成回调
                    listener.onComplete(parameter);
                } else {
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

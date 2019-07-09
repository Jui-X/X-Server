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

    private volatile long lastReadTime = System.currentTimeMillis();
    private volatile long lastWriteTime = System.currentTimeMillis();

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

        // 进行CallBack状态监测，判断是否处于自循环状态
        inputCallback.checkAttachNull();

        // 将channel注册到Selector的对应事件（此处为读事件）上
        return IOProvider.registerInput(channel, inputCallback);
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    private final IOProvider.HandleProvideCallback inputCallback = new IOProvider.HandleProvideCallback() {
        @Override
        protected void provideParameter(IOParameter parameter) {
            if (isClosed.get()) {
                return;
            }

            lastReadTime = System.currentTimeMillis();

            IOParameter.IOParaEventProcessor processor = receiveIOEventProcessor;
            if (processor == null) {
                return;
            }

            if (parameter == null) {
                parameter = processor.provideParameter();
            }

            try {
                // 具体的读取操作
                if (parameter == null) {
                    processor.onConsumeFailed(null, new IOException("Provide IOParameter is null..."));
                }  else {
                    int count = parameter.readFrom(channel);
                    if (count == 0) {
                        System.out.println("Current read no data!");
                    }

                    // 检查是否还有空闲区间，以及是否需要填满空闲区间
                    if (parameter.remained() && parameter.isNeedConsumingRemaining()) {
                        // 附加未消费完的parameter
                        attach = parameter;
                        // 再次注册数据发送事件
                        IOProvider.registerInput(channel, this);
                    } else {
                        parameter = null;
                        // 读取完成回调
                        processor.onConsumeCompleted(parameter);
                    }
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

        // 进行CallBack状态监测，判断是否处于自循环状态
        outputCallback.checkAttachNull();

        // 当前发送的数据添加到回调中
        return IOProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

    private final IOProvider.HandleProvideCallback outputCallback = new IOProvider.HandleProvideCallback() {
        @Override
        protected void provideParameter(IOParameter parameter) {
            if (isClosed.get()) {
                return;
            }

            lastWriteTime = System.currentTimeMillis();

            IOParameter.IOParaEventProcessor processor = sendIOEventProcessor;
            if (processor == null) {
                return;
            }

            // 从回调中得到发送数据的parameter
            if (parameter == null) {
                parameter = processor.provideParameter();
            }

            try {
                if (parameter == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideParameter is null"));
                } else {
                    int count = parameter.writeTo(channel);
                    if (count == 0) {
                        System.out.println("Current write no data!");
                    }

                    // 检查是否还有空闲区间，以及是否需要填满空闲区间
                    if (parameter.remained() && parameter.isNeedConsumingRemaining()) {
                        // 附加未消费完的parameter
                        attach = parameter;
                        // 再次注册数据发送事件
                        IOProvider.registerOutput(channel, this);
                    } else {
                        parameter = null;
                        // 写入完成回调
                        processor.onConsumeCompleted(parameter);
                    }
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

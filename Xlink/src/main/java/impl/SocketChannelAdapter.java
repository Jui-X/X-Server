package impl;

import Utils.CloseUtils;
import core.IOParameter;
import core.IOProvider;
import core.Receiver;
import core.Sender;
import impl.exceptions.EmptyIOParameterException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
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
    private final AbsProvideCallback inputCallback;
    private final AbsProvideCallback outputCallback;

    public SocketChannelAdapter(SocketChannel channel, core.IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;

        this.IOProvider = ioProvider;
        this.listener = listener;

        this.inputCallback = new InputProviderCallback(IOProvider, channel, SelectionKey.OP_READ);
        this.outputCallback = new OutputProviderCallback(IOProvider, channel, SelectionKey.OP_WRITE);
    }

    @Override
    public void setReceiveListener(IOParameter.IOParaEventProcessor processor) {
        inputCallback.eventProcessor = processor;
    }

    @Override
    public long getLastReadTime() {
        return inputCallback.lastActiveTime;
    }

    @Override
    public void postReceiveAsync() throws Exception {
        if (isClosed.get() || !channel.isOpen()) {
            throw new IOException("Current Thread has been closed.");
        }

        // 进行CallBack状态监测，判断是否处于自循环状态
        inputCallback.checkAttachNull();

        // 将channel注册到Selector的对应事件（此处为读事件）上
        IOProvider.register(inputCallback);
    }

    @Override
    public void setSendProcessor(IOParameter.IOParaEventProcessor processor) {
        outputCallback.eventProcessor = processor;
    }

    @Override
    public long getLastWriteTime() {
        return outputCallback.lastActiveTime;
    }

    @Override
    public void postSendAsync() throws Exception {
        if (isClosed.get()) {
            throw new IOException("Current Thread has been closed.");
        }

        // 进行CallBack状态监测，判断是否处于自循环状态
        outputCallback.checkAttachNull();

        // 当前发送的数据添加到回调中
        IOProvider.register(outputCallback);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // 接触注册回调
            IOProvider.unRegister(channel);
            // 关闭
            CloseUtils.close(channel);
            // 回调返回当前Channel已关闭的信息
            listener.onChannelClosed(channel);
        }
    }

    abstract class AbsProvideCallback extends core.IOProvider.HandleProvideCallback {
        volatile IOParameter.IOParaEventProcessor eventProcessor;
        volatile long lastActiveTime = System.currentTimeMillis();

        AbsProvideCallback(core.IOProvider provider, SocketChannel channel, int ops) {
            super(provider, channel, ops);
        }

        @Override
        protected boolean provideParameter(IOParameter parameter) {
            if (isClosed.get()) {
                return false;
            }

            final IOParameter.IOParaEventProcessor processor = eventProcessor;
            if (processor == null) {
                return false;
            }

            // 刷新输出时间
            lastActiveTime = System.currentTimeMillis();

            if (parameter == null) {
                // parameter为空则拿一份新的parameter
                parameter = processor.provideParameter();
            }

            try {
                // 具体的读取操作
                if (parameter == null) {
                    throw new EmptyIOParameterException("Provide IOParameter is null...");
                }
                int count = consumeIOParameter(parameter, channel);

                // 检查是否还有空闲区间，以及是否需要填满空闲区间
                // 1.本次数据未消费
                // 2.需要消费所有数据
                if (parameter.remained() && (count == 0 || parameter.isNeedConsumingRemaining())) {
                    // 附加未消费完的parameter
                    attach = parameter;
                    // 再次注册数据发送事件
                    return true;
                } else {
                    parameter = null;
                    // 读取完成回调
                    return processor.onConsumeCompleted(parameter);
                }

            } catch (IOException e) {
                if (processor.onConsumeFailed(e)) {
                    CloseUtils.close(SocketChannelAdapter.this);
                }
                return false;
            }
        }

        @Override
        public void fireThrowable(Throwable e) {
            final IOParameter.IOParaEventProcessor processor = eventProcessor;
            if (processor == null || processor.onConsumeFailed(e)) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }

        protected abstract int consumeIOParameter(IOParameter parameter, SocketChannel channel) throws IOException;
    }

    class InputProviderCallback extends AbsProvideCallback {

        InputProviderCallback(core.IOProvider provider, SocketChannel channel, int ops) {
            super(provider, channel, ops);
        }

        @Override
        protected int consumeIOParameter(IOParameter parameter, SocketChannel channel) throws IOException {
            return parameter.readFrom(channel);
        }
    }

    class OutputProviderCallback extends AbsProvideCallback {

        OutputProviderCallback(core.IOProvider provider, SocketChannel channel, int ops) {
            super(provider, channel, ops);
        }

        @Override
        protected int consumeIOParameter(IOParameter parameter, SocketChannel channel) throws IOException {
            return parameter.writeTo(channel);
        }
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}

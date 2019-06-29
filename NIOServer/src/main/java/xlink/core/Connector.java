package xlink.core;

import xlink.impl.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @param: none
 * @description: Connector连接类
 *               连接依赖于SocketChannel
 *               SocketChannel是Sender和Receiver的实现
 *               基于Socket连接之上进行二次封装
 *               封装成一个真实的发送者和接收者
 *               最终的核心是经过SocketChannel
 *               在其之上经过一系列的调度和完善
 * @author: KingJ
 * @create: 2019-06-02 13:32
 **/
public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setUp(SocketChannel channel) throws IOException {
        this.channel = channel;

        IOContext ioContext = IOContext.getInstance();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, ioContext.getProvider(),
                this);

        this.sender = adapter;
        this.receiver = adapter;

        readNextMsg();
    }

    private void readNextMsg() {
        if (receiver != null) {
            try {
                // 将listener传入进去
                // 在接受完成的进行回调
                // 执行onComplete方法 打印（receiveNewMessage） 并 继续接收数据（readNextMsg）
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                System.out.println("Connector => 接收数据异常：" + e.getMessage());
            }
        }
    }

    private IOParameter.IOParaEventListener echoReceiveListener = new IOParameter.IOParaEventListener() {
        @Override
        public void onStart(IOParameter parameter) {

        }

        @Override
        public void onComplete(IOParameter parameter) {
            receiveNewMessage(parameter.bufferToString());
            readNextMsg();
        }
    };

    protected void receiveNewMessage(String msg) {
        System.out.println("Connector => UUID: " + key.toString() + ": " + msg);
    }

    @Override
    public void close() {

    }

    /**
     * Connector继承自SocketChannelAdapter.OnChannelStatusChangedListener
     * 实现onChannelClosed方法
     * 以完成在异常发生时的操作
     * @param channel
     */
    @Override
    public void onChannelClosed(SocketChannel channel) {

    }
}

package xlink.core;

import xlink.impl.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @param: none
 * @description:
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

    private void readNextMsg() {
        if (receiver != null) {
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                System.out.println("接收数据异常：" + e.getMessage());
            }
        }
    }

    protected void receiveNewMessage(String msg) {
        System.out.println(key.toString() + msg);
    }

    @Override
    public void close() {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }
}

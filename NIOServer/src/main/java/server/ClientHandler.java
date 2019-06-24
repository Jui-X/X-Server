package server;


import common.utils.CloseUtils;
import xlink.core.Connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @param: none
 * @description: 处理客户端请求
 * @author: KingJ
 * @create: 2019-05-27 20:14
 **/
public class ClientHandler {
    private Connector connector;
    private final SocketChannel socketChannel;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallBack clientHandlerCallBack;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.socketChannel = socketChannel;

        connector = new Connector() {
            @Override
            protected void receiveNewMessage(String msg) {
                super.receiveNewMessage(msg);
                clientHandlerCallBack.onNewMessageArrived(ClientHandler.this, msg);
            }

            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);
                closeItself();
            }
        };
        connector.setUp(socketChannel);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo = "Address: " + socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public interface ClientHandlerCallBack {
        // 关闭自身线程
        void selfClose(ClientHandler clientHandler);
        // 收到消息时转发给所有其余客户端
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }

    /**
     *
     * 负责写入回送到客户端的信息
     *
     **/
    class ClientWriteHandler extends Thread {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            // TODO ThreadExecutorPool创建线程池时自定义ThreadFactory
            this.executorService = new ThreadPoolExecutor(5, 5, 1000
                    , TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        }

        class WriteRunnable implements Runnable {
            private final String str;

            WriteRunnable(String str) {
                this.str = str + "\n";
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(str.getBytes());
                // 反转是因为ByteBuffer再发送数据时，从position指针所在位置开始发送到结束位置
                // 所以需要反转将position回到初始位置，并将结束位置置为之前的position所在位置
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        int len = socketChannel.write(byteBuffer);
                        // len == 0合法
                        if (len < 0) {
                            System.out.println("客户端已无法发送数据！");
                            ClientHandler.this.closeItself();
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        void send(String str) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }
    }

    public void sendMsg(String str) {
        writeHandler.send(str);
    }

    private void closeItself() {
        exit();
        clientHandlerCallBack.selfClose(this);
    }

    public void exit() {
        CloseUtils.close(connector);
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }
}

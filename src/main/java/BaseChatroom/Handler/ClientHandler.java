package BaseChatroom.Handler;

import BaseChatroom.Utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
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
    private final SocketChannel socketChannel;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallBack clientHandlerCallBack;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.socketChannel = socketChannel;
        // 设置非阻塞模式
        socketChannel.configureBlocking(false);

        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ClientReadHandler(readSelector);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo = "Address: " + socketChannel.getLocalAddress().toString();
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
     * 负责读取客户端发来的信息
     *
     **/

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            super.run();

            // 处理客户端请求
            try {
                do {
                    // 轮询结果，返回已就绪的Channel数量
                    // 0表示没有Channel就绪
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            // 读取ByteBuffer之前先清空
                            byteBuffer.clear();
                            // 读取
                            int read = client.read(byteBuffer);
                            if (read > 0) {
                                //
                                String str = new String(byteBuffer.array(), 0, read - 1);
                                clientHandlerCallBack.onNewMessageArrived(ClientHandler.this, str);
                            } else {
                                System.out.println("Client cannot read data now!");
                                // 退出当前客户端
                                ClientHandler.this.closeItself();
                                break;
                            }
                        }
                    }
                } while (!done);
            } catch (IOException e) {
                if (!done) {
                    System.out.println("TCP连接异常断开");
                    ClientHandler.this.closeItself();
                }
            } finally {
                CloseUtils.close(selector);
            }
        }

        void exit() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
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
                this.str = str.substring(0, str.length() - 1) + "\n";
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

    public void receiveMsg() {
        readHandler.start();
    }

    private void closeItself() {
        exit();
        clientHandlerCallBack.selfClose(this);
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }
}

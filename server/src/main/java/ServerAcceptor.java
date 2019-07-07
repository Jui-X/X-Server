import Utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 21:29
 **/
public class ServerAcceptor extends Thread {
    private final AcceptListener listener;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Selector selector;
    private boolean done = false;

    public ServerAcceptor(AcceptListener listener) throws IOException {
        super("Server-Acceptor-Thread");
        this.listener = listener;
        this.selector = Selector.open();
    }

    boolean awaitRunning() {
        try {
            latch.wait();
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void run() {
        super.run();

        // 回调告知外部已进入运行
        latch.countDown();

        Selector selector = this.selector;
        // 等待客户端连接
        do {
            try {
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

                    // 关注当前Key的状态是否是已建立连接的状态
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        // 建立客户端过来的SocketChannel
                        SocketChannel socketChannel = server.accept();
                        listener.newSocketArrived(socketChannel);
                    }
                }
            } catch (IOException e) {
                continue;
            }

        } while (!done);
        System.out.println("ServerAcceptor => ServerAcceptor finished.");
    }

    void exit() {
        done = true;
        // 直接关闭
        CloseUtils.close(selector);
    }

    interface AcceptListener {
        void newSocketArrived(SocketChannel channel);
    }
}


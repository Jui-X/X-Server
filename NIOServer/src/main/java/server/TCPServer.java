package server;

import common.utils.CloseUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPServer implements ClientHandler.ClientHandlerCallBack{
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> clientHandlerList = new ArrayList<ClientHandler>();
    private final ExecutorService forwardThreadPoolExecutor;
    private Selector selector;
    private ServerSocketChannel server;

    public TCPServer(int port) {
        this.port = port;
        this.forwardThreadPoolExecutor = new ThreadPoolExecutor(5, 5,
                10000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(),
                new IOProviderThreadFactory("Forward-Thread-Pool"));
    }

    public boolean start() {
        try {
            selector = Selector.open();

            ServerSocketChannel server = ServerSocketChannel.open();
            // 将ServerSocketChannel配置为非阻塞
            server.configureBlocking(false);
            // 将Channel对应socket绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            // 注册客户端连接到达的监听
            server.register(selector, SelectionKey.OP_ACCEPT);

            this.server = server;
            System.out.println("TCPServer => 服务器信息：" + server.getLocalAddress().toString());

            ClientListener clientListener = new ClientListener();
            listener = clientListener;

            // 服务端开始监听客户端过来的消息
            clientListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }

        CloseUtils.close(selector);
        CloseUtils.close(server);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }
        // 关闭线程池
        forwardThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.sendMsg(str);
        }
    }

    @Override
    public synchronized void selfClose(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler clientHandler, final String msg) {
//        System.out.println("TCPServer => Received from " + clientHandler.getClientInfo());

        // 线程池异步提交转发任务
        forwardThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                clientHandlerList.stream().filter(h -> !h.equals(clientHandler))
                        .forEach(h -> h.sendMsg(msg));
            }
        });
    }

    // 监听事件类
    private class ClientListener extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            super.run();

            Selector selector = TCPServer.this.selector;
            System.out.println("TCPServer => TCP Server is ready.");
            // 等待客户端连接
            do {
                try {
                    // 轮询结果，返回已就绪的Channel数量
                    // 0表示没有Channel就绪
                    if (selector.select() == 0) {
                        // 处于结束态的话直接返回
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    // 当前就绪时间的迭代器
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }

                        SelectionKey key = iterator.next();
                        // 已经使用过的key从迭代器中移除
                        iterator.remove();

                        // 关注当前Key的状态是否是已建立连接的状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            // 客户端过来的SocketChannel，并在NIO机制下key已经是可用的，可直接接收数据
                            SocketChannel client = server.accept();

                            // 服务端异步构建线程处理请求
                            try {
                                // 构建新的ClientHandler线程处理客户端请求
                                ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);
                                // 添加同步处理
                                synchronized (TCPServer.this) {
                                    clientHandlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("TCPServer => TCP Client Error. " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    continue;
                }

            } while (!done);
            System.out.println("TCPServer => TCP Server exit.");
        }

        void exit() {
            done = true;
            // 唤醒当前阻塞
            selector.wakeup();
        }
    }

    static class IOProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IOProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}

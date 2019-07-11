import Utils.CloseUtils;
import core.Connector;
import core.IOContext;
import handler.ConnectorCloseChain;
import handler.ConnectorHandler;
import impl.IOSelectorProvider;
import impl.IOStealingSelectorProvider;
import x.Xyz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-06 20:39
 **/
public class UDPClientTest {
    // 不考虑消耗发送
    // 并发量：2000 * 4 / 400 * 100 = 2w/s，算上来回两次数据解析：4w/s
    private static final int CLIENT_SIZE = 2000;
    private static final int SEND_THREAD_SIZE = 4;
    private static final int SEND_THREAD_DELAY = 400;
    private static volatile boolean done;

    public static void main(String[] args) throws IOException {
        ServerInfo info = UDPClientSearcher.searchServer(5000);
        System.out.println("UDPClientTest => Server:" + info);
        if (info == null) {
            return;
        }

        // 缓存文件目录
        File cachePath = Xyz.getCacheDir("client/test");
        IOContext.setup()
                .ioProvider(new IOStealingSelectorProvider(1))
                .start();

        // 当前连接数量
        int size = 0;
        final List<TCPClient> tcpClientList = new ArrayList<>(CLIENT_SIZE);

        // 关闭时移除
        final ConnectorCloseChain closeChain = new ConnectorCloseChain() {
            @Override
            protected boolean consume(ConnectorHandler handler, Connector connector) {
                tcpClientList.remove(handler);
                if (tcpClientList.size() == 0) {
                    CloseUtils.close(System.in);
                }
                return false;
            }
        };

        for (int i = 0; i < CLIENT_SIZE; i++) {
            try {
                TCPClient client = TCPClient.startWith(info, cachePath, false);
                if (client == null) {
                    throw new NullPointerException();
                }
                // 添加关闭链式节点
                client.getCloseChain().appendLast(closeChain);
                tcpClientList.add(client);
                System.out.println("UDPClientTest => 连接成功：" + (++size));
            } catch (IOException | NullPointerException e) {
                System.out.println("UDPClientTest => 连接异常！");
                break;
            }
        }

        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                TCPClient[] copyClients = tcpClientList.toArray(new TCPClient[0]);
                for (TCPClient client : copyClients) {
                    client.send("Hello！");
                }
                if (SEND_THREAD_DELAY > 0) {
                    try {
                        Thread.sleep(SEND_THREAD_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        List<Thread> threads = new ArrayList<>(SEND_THREAD_SIZE);
        for (int i = 0; i < SEND_THREAD_SIZE; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }

        System.in.read();

        // 等待线程完成
        done = true;

        // 客户端结束操作
        TCPClient[] copyClients = tcpClientList.toArray(new TCPClient[0]);
        for (TCPClient client : copyClients) {
            client.exit();
        }

        // 关闭框架线程池
        IOContext.close();

        // 强制结束处于等待的线程
        for (Thread thread : threads) {
            try {
                thread.interrupt();
            } catch (Exception ignored) {
            }
        }
    }
}

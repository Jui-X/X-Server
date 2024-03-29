package client;

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
    private static int done = 0;

    public static void main(String[] args) throws IOException {
        ServerInfo info = UDPClientSearcher.searchServer(10000);
        System.out.println("UDPClientTest => Server:" + info);

        if (info == null) {
            return;
        }

        // 当前连接数量
        int size = 0;
        final List<TCPClient> tcpClientList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            try {
                TCPClient client = TCPClient.startWith(info);
                if (client == null) {
                    System.out.println("连接异常！");
                    continue;
                }

                tcpClientList.add(client);
                System.out.println("连接成功：" + (++size));
            } catch (IOException e) {
                System.out.println("连接异常！");
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.in.read();

        Runnable runnable = () -> {
            while (done < 3) {
                for (TCPClient client : tcpClientList) {
                    client.sendMsg("Hello！");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                done++;
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        System.in.read();

        // 等待线程完成
        done = 3;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (TCPClient client : tcpClientList) {
            client.exit();
        }
    }
}

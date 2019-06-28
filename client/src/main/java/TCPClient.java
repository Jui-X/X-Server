import Utils.CloseUtils;
import core.Connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPClient extends Connector {

    public TCPClient(SocketChannel socketChannel) throws IOException {
        setUp(socketChannel);
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
        SocketChannel client = SocketChannel.open();

        // 连接到TCPServer指定的端口
        client.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("客户端信息：" + client.getLocalAddress().toString());
        System.out.println("服务器端信息：" + client.getRemoteAddress().toString());

        try {
            return new TCPClient(client);
        } catch (Exception e) {
            System.out.println("连接异常关闭");
            CloseUtils.close(client);
        }

        client.close();
        System.out.println("TCP Client exit...");

        return null;
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已关闭，无法读取数据！");
    }

    public void exit() {
        CloseUtils.close(this);
    }

    /**
     *
     * 负责读取信息
     *
     **/

    static class ClientReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            // 客户端请求处理
            try {
                // 得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str;
                    try {
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }

                    if (str == null) {
                        System.out.println("连接超时，连接已关闭，无法读取数据");
                        break;
                    }
                    System.out.println(str);
                } while (!done);

            } catch (IOException e) {
                if (!done) {
                    System.out.println("TCP连接异常断开");
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}

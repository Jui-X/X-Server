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

        System.out.println("TCPClient => 客户端信息：" + client.getLocalAddress().toString());
        System.out.println("TCPClient => 服务器端信息：" + client.getRemoteAddress().toString());

        try {
            return new TCPClient(client);
        } catch (Exception e) {
            System.out.println("TCPClient => 连接异常关闭");
            CloseUtils.close(client);
        }

        client.close();
        System.out.println("TCPClient => TCP Client exit...");

        return null;
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("TCPClient => 连接已关闭，无法读取数据！");
    }

    public void exit() {
        CloseUtils.close(this);
    }

}

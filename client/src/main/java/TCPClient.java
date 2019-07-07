import Utils.CloseUtils;
import core.Connector;
import core.Packet;
import core.ReceivePacket;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPClient extends Connector {
    private final File cachePath;

    public TCPClient(SocketChannel socketChannel, File path) throws IOException {
        cachePath = path;
        setUp(socketChannel);
    }

    public static TCPClient startWith(ServerInfo info, File cachePath) throws IOException {
        SocketChannel client = SocketChannel.open();

        // 连接到TCPServer指定的端口
        client.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("TCPClient => 客户端信息：" + client.getLocalAddress().toString());
        System.out.println("TCPClient => 服务器端信息：" + client.getRemoteAddress().toString());

        try {
            return new TCPClient(client, cachePath);
        } catch (Exception e) {
            System.out.println("TCPClient => 连接异常关闭");
            CloseUtils.close(client);
        }

        client.close();
        System.out.println("TCPClient => TCP Client exit...");

        return null;
    }

    @Override
    protected File createNewReceiveFile() {
        return Xyz.createTempFile(cachePath);
    }

    @Override
    protected void receiveNewPacket(ReceivePacket packet) {
        super.receiveNewPacket(packet);
        /*if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) packet.entity();
        }*/
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

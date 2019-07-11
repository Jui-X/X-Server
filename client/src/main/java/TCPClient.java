import Utils.CloseUtils;
import box.StringReceivePacket;
import core.Connector;
import core.Packet;
import core.ReceivePacket;
import handler.ConnectorHandler;
import handler.ConnectorStringPacketChain;
import x.Xyz;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPClient extends ConnectorHandler {

    public TCPClient(SocketChannel socketChannel, File path, boolean printReceiveString) throws IOException {
        super(path, socketChannel);
        if (printReceiveString) {
            getStringPacketChain().appendLast(new PrintStringPacketChain());
        }
    }

    private class PrintStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            System.out.println(str);
            return true;
        }
    }

    public static TCPClient startWith(ServerInfo info, File cachePath) throws IOException {
        return startWith(info, cachePath, true);
    }

    public static TCPClient startWith(ServerInfo info, File cachePath, boolean printReceiveString) throws IOException {
        SocketChannel client = SocketChannel.open();

        // 连接到TCPServer指定的端口
        client.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("TCPClient => 客户端信息：" + client.getLocalAddress().toString());
        System.out.println("TCPClient => 服务器端信息：" + client.getRemoteAddress().toString());

        try {
            return new TCPClient(client, cachePath, printReceiveString);
        } catch (Exception e) {
            System.out.println("TCPClient => 连接异常关闭");
            CloseUtils.close(client);
        }

        client.close();
        System.out.println("TCPClient => TCP Client exit...");

        return null;
    }

    @Override
    protected void receiveNewPacket(ReceivePacket packet) {
        super.receiveNewPacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) packet.entity();
            System.out.println(string);
        }
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

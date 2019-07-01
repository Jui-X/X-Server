import Utils.CloseUtils;
import core.Connector;
import core.Packet;
import core.ReceivePacket;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description: 处理客户端请求
 * @author: KingJ
 * @create: 2019-05-27 20:14
 **/
public class ClientHandler extends Connector {
    // 缓存文件目录
    private final File cachePath;
    private final ClientHandlerCallBack clientHandlerCallBack;
    private final String clientInfo;

    public ClientHandler(File cachePath, SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.cachePath = cachePath;
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo = "Address: " + socketChannel.getLocalAddress().toString();

        System.out.println("ClientHandler => 新客户端连接：" + clientInfo);

        setUp(socketChannel);
    }

    @Override
    protected void receiveNewPacket(ReceivePacket packet) {
        super.receiveNewPacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String string = (String) packet.entity();
            System.out.println(key.toString() + ": "+ string);
            // 将收到的String进行转发
            clientHandlerCallBack.onNewMessageArrived(this, string);
        }
    }

    public interface ClientHandlerCallBack {
        // 关闭自身线程
        void selfClose(ClientHandler clientHandler);
        // 收到消息时转发给所有其余客户端
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }

    @Override
    protected File createNewReceiveFile() {
        return Xyz.createTempFile(cachePath);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeItself();
    }

    private void closeItself() {
        exit();
        clientHandlerCallBack.selfClose(this);
    }

    public void exit() {
        CloseUtils.close(this);
        System.out.println("ClientHandler => 客户端已退出：" + clientInfo);
    }
}

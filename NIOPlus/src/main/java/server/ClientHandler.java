package server;

import common.utils.CloseUtils;
import xlink.core.Connector;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description: 处理客户端请求
 * @author: KingJ
 * @create: 2019-05-27 20:14
 **/
public class ClientHandler extends Connector {
    private final ClientHandlerCallBack clientHandlerCallBack;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo = "Address: " + socketChannel.getLocalAddress().toString();

        System.out.println("ClientHandler => 新客户端连接：" + clientInfo);

        setUp(socketChannel);
    }

    @Override
    protected void receiveNewMessage(String msg) {
        super.receiveNewMessage(msg);
        clientHandlerCallBack.onNewMessageArrived(this, msg);
    }

    public interface ClientHandlerCallBack {
        // 关闭自身线程
        void selfClose(ClientHandler clientHandler);
        // 收到消息时转发给所有其余客户端
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
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

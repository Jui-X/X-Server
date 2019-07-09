package handler;

import Utils.CloseUtils;
import box.StringReceivePacket;
import core.Connector;
import core.Packet;
import core.ReceivePacket;
import x.Xyz;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

/**
 * @param: none
 * @description: 处理客户端请求
 * @author: KingJ
 * @create: 2019-05-27 20:14
 **/
public class ConnectorHandler extends Connector {
    // 缓存文件目录
    private final File cachePath;
    private final Executor deliveryPool;
    private final String clientInfo;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();

    public ConnectorHandler(File cachePath, SocketChannel socketChannel, Executor deliveryPool) throws IOException {
        this.cachePath = cachePath;
        this.deliveryPool = deliveryPool;
        this.clientInfo = "Address: " + socketChannel.getLocalAddress().toString();

        System.out.println("handler.ConnectorHandler => 新客户端连接：" + clientInfo);

        setUp(socketChannel);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    @Override
    protected void receiveNewPacket(ReceivePacket packet) {
        super.receiveNewPacket(packet);
        switch (packet.type()) {
            case Packet.TYPE_MEMORY_STRING:
                deliveryStringPacket((StringReceivePacket) packet);
                break;
            default:
                System.out.println("New Packet: " + packet.type() + " - " + packet.length());
        }
    }

    private void deliveryStringPacket(StringReceivePacket packet) {
        deliveryPool.execute(() -> stringPacketChain.handle(ConnectorHandler.this, packet));
    }

    @Override
    protected File createNewReceiveFile(long length, byte[] headInfo) {
        return Xyz.createTempFile(cachePath);
    }

    @Override
    protected OutputStream createNewReceiveDirectOutputStream(long length, byte[] headInfo) {
        // 服务器默认创建一个内存存储ByteArrayOutputStream
        return new ByteArrayOutputStream();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this, this);
    }

    public void exit() {
        CloseUtils.close(this);
        closeChain.handle(this, this);
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }

    public ConnectorCloseChain getCloseChain() {
        return closeChain;
    }
}

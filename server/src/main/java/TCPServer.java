import Utils.CloseUtils;
import box.StringReceivePacket;
import core.Connector;
import core.ScheduleJob;
import core.schedule.IdleTimeoutScheduleJob;
import handler.ClientHandler;
import handler.ConnectCloseChain;
import handler.ConnectorStringPacketChain;
import x.Xyz;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-27 16:39
 **/
public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {
    private final int port;
    // 缓存文件目录
    private final File cachePath;
    private final ExecutorService forwardThreadPoolExecutor;
    private final List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final Map<String, Group> groups = new HashMap<>();
    private Selector selector;
    private ServerAcceptor acceptor;
    private ServerSocketChannel server;
    private final ServerStatistics statistics = new ServerStatistics();


    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        this.forwardThreadPoolExecutor = new ThreadPoolExecutor(5, 5,
                10000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        this.groups.put(Xyz.GROUP_NAME, new Group(Xyz.GROUP_NAME, this));
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
            this.acceptor = acceptor;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }

        ClientHandler[] clientHandlers;
        synchronized (clientHandlerList) {
            clientHandlers = clientHandlerList.toArray(new ClientHandler[0]);
            clientHandlerList.clear();
        }
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.exit();
        }

        CloseUtils.close(server);
        // 关闭线程池
        forwardThreadPoolExecutor.shutdownNow();
    }

    void broadcast(String str) {
        str = "系统通知：" + str;

        ClientHandler[] clientHandlers;
        synchronized (clientHandlerList) {
            clientHandlers = clientHandlerList.toArray(new ClientHandler[0]);
        }
        for (ClientHandler clientHandler : clientHandlers) {
            sendMessageToClient(clientHandler, str);
        }
    }

    @Override
    public void sendMessageToClient(ClientHandler handler, String msg) {
        handler.send(msg);
        statistics.sendSize++;
    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectCloseChain {

        @Override
        protected boolean consume(ClientHandler handler, Connector connector) {
            synchronized (clientHandlerList) {
                clientHandlerList.remove(handler);
                // 移除群聊的客户端
                Group group = groups.get(Xyz.GROUP_NAME);
                group.removeMember(handler);
            }
            return false;
        }
    }

    @Override
    public void newSocketArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(cachePath, channel, forwardThreadPoolExecutor);
            System.out.println(clientHandler.getClientInfo() + " Connect!");

            // 添加收到消息的处理责任链
            clientHandler.getStringPacketChain()
                    .appendLast(statistics.statisticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            // 添加关闭链接时的责任链
            clientHandler.getCloseChain()
                    .appendLast(new RemoveQueueOnConnectorClosedChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.MILLISECONDS, clientHandler);
            clientHandler.schedule(scheduleJob);

            synchronized (clientHandlerList) {
                clientHandlerList.add(clientHandler);
                System.out.println("当前客户端数量：" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端链接异常：" + e.getMessage());
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            if (str.startsWith(Xyz.COMMAND_GROUP_JOIN)) {
                Group group = groups.get(Xyz.GROUP_NAME);
                if (group.addMember(handler)) {
                    sendMessageToClient(handler, "Join Group: " + group.getName());
                }
                return true;
            } else if (str.startsWith(Xyz.COMMAND_GROUP_LEAVE)) {
                Group group = groups.get(Xyz.GROUP_NAME);
                if (group.removeMember(handler)) {
                    sendMessageToClient(handler, "Leave Group: " + group.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            // 捡漏模式，当我们第一遍未消费，然后也没有加入到群，自然没有后续的节点消费
            // 此时我们进行二次消费，返回发送过来的消息
            sendMessageToClient(handler, stringReceivePacket.entity());
            return true;
        }
    }
}

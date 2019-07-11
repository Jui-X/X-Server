import Utils.CloseUtils;
import audio.AudioRoom;
import box.StringReceivePacket;
import core.Connector;
import core.ScheduleJob;
import core.schedule.IdleTimeoutScheduleJob;
import handler.ConnectorHandler;
import handler.ConnectorCloseChain;
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
    private final List<ConnectorHandler> connectorHandlerList = new ArrayList<>();
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

        ConnectorHandler[] connectorHandlers;
        synchronized (connectorHandlerList) {
            connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
            connectorHandlerList.clear();
        }
        for (ConnectorHandler connectorHandler : connectorHandlers) {
            connectorHandler.exit();
        }

        CloseUtils.close(server);
        // 关闭线程池
        forwardThreadPoolExecutor.shutdownNow();
    }

    void broadcast(String str) {
        str = "系统通知：" + str;

        ConnectorHandler[] connectorHandlers;
        synchronized (connectorHandlerList) {
            connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
        }
        for (ConnectorHandler connectorHandler : connectorHandlers) {
            sendMessageToClient(connectorHandler, str);
        }
    }

    @Override
    public void sendMessageToClient(ConnectorHandler handler, String msg) {
        handler.send(msg);
        statistics.sendSize++;
    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {

        @Override
        protected boolean consume(ConnectorHandler handler, Connector connector) {
            synchronized (connectorHandlerList) {
                connectorHandlerList.remove(handler);
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
            ConnectorHandler connectorHandler = new ConnectorHandler(cachePath, channel);
            System.out.println(connectorHandler.getClientInfo() + " Connect!");

            // 添加收到消息的处理责任链
            connectorHandler.getStringPacketChain()
                    .appendLast(statistics.statisticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain())
                    .appendLast(new ParseAudioStreamCommandStringPacketChain());

            // 添加关闭链接时的责任链
            connectorHandler.getCloseChain()
                    .appendLast(new RemoveAudioQueueOnConnectorClosedChain())
                    .appendLast(new RemoveQueueOnConnectorClosedChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.MILLISECONDS, connectorHandler);
            connectorHandler.schedule(scheduleJob);

            synchronized (connectorHandlerList) {
                connectorHandlerList.add(connectorHandler);
                System.out.println("当前客户端数量：" + connectorHandlerList.size());
            }

            // 回送客户端在服务器的唯一标识
            sendMessageToClient(connectorHandler, Xyz.COMMAND_INFO_NAME + connectorHandler.getKey().toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端链接异常：" + e.getMessage());
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
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
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            // 捡漏模式，当我们第一遍未消费，然后也没有加入到群，自然没有后续的节点消费
            // 此时我们进行二次消费，返回发送过来的消息
            sendMessageToClient(handler, stringReceivePacket.entity());
            return true;
        }
    }

    /**
     * 从全部列表中通过Key查询到一个链接
     */
    private ConnectorHandler finConnectorFromKey(String key) {
        synchronized (connectorHandlerList) {
            for (ConnectorHandler connectorHandler : connectorHandlerList) {
                if (connectorHandler.getKey().toString().equals(key)) {
                    return connectorHandler;
                }
            }
        }
        return null;
    }

    // 音频命令控制与数据流传输链接映射表
    private final HashMap<ConnectorHandler, ConnectorHandler> audioCmdToStreamMap = new HashMap<>(100);
    private final HashMap<ConnectorHandler, ConnectorHandler> audioStreamToCmdMap = new HashMap<>(100);
    // 房间映射表, 房间号-房间的映射
    private final HashMap<String, AudioRoom> audioRoomMap = new HashMap<>(50);
    // 链接与房间的映射表，音频链接-房间的映射
    private final HashMap<ConnectorHandler, AudioRoom> audioStreamRoomMap = new HashMap<>(100);

    /**
     * 通过音频命令控制链接寻找数据传输流链接, 未找到则发送错误
     */
    private ConnectorHandler findAudioStreamConnector(ConnectorHandler handler) {
        ConnectorHandler connectorHandler = audioCmdToStreamMap.get(handler);
        if (connectorHandler == null) {
            sendMessageToClient(handler, Xyz.COMMAND_INFO_AUDIO_ERROR);
            return null;
        } else {
            return connectorHandler;
        }
    }

    /**
     * 通过音频数据传输流链接寻找命令控制链接
     */
    private ConnectorHandler findAudioCmdConnector(ConnectorHandler handler) {
        return audioStreamToCmdMap.get(handler);
    }

    /**
     * 生成一个当前缓存列表中没有的房间
     */
    private AudioRoom createNewRoom() {
        AudioRoom room;
        do {
            room = new AudioRoom();
        } while (audioRoomMap.containsKey(room.getRoomCode()));
        // 添加到缓存列表
        audioRoomMap.put(room.getRoomCode(), room);
        return room;
    }

    /**
     * 加入房间
     *
     * @return 是否加入成功
     */
    private boolean joinRoom(AudioRoom room, ConnectorHandler streamConnector) {
        if (room.enterRoom(streamConnector)) {
            audioStreamRoomMap.put(streamConnector, room);
            return true;
        }
        return false;
    }

    /**
     * 解散房间
     *
     * @param streamConnector 解散者
     */
    private void dissolveRoom(ConnectorHandler streamConnector) {
        AudioRoom room = audioStreamRoomMap.get(streamConnector);
        if (room == null) {
            return;
        }

        ConnectorHandler[] connectors = room.getConnectors();
        for (ConnectorHandler connector : connectors) {
            // 接触桥接
            connector.unBindToBridge();
            // 移除缓存
            audioStreamRoomMap.remove(connector);
            if (connector != streamConnector) {
                // 退出房间并获取对方
                sendStreamConnectorMessage(connector, Xyz.COMMAND_INFO_AUDIO_STOP);
            }
        }

        // 销毁房间
        audioRoomMap.remove(room.getRoomCode());
    }

    /**
     * 给链接流对应的命令控制链接发送信息
     */
    private void sendStreamConnectorMessage(ConnectorHandler streamConnector, String msg) {
        if (streamConnector != null) {
            ConnectorHandler audioCmdConnector = findAudioCmdConnector(streamConnector);
            sendMessageToClient(audioCmdConnector, msg);
        }
    }

    /**
     * 音频命令解析
     */
    private class ParseAudioStreamCommandStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            if (str.startsWith(Xyz.COMMAND_CONNECTOR_BIND)) {
                String key = str.substring(Xyz.COMMAND_CONNECTOR_BIND.length());
                ConnectorHandler audioStreamConnector = finConnectorFromKey(key);
                if (audioStreamConnector != null) {
                    // 添加绑定关系
                    audioCmdToStreamMap.put(handler, audioStreamConnector);
                    audioStreamToCmdMap.put(audioStreamConnector, handler);

                    // 转换为桥接模式
                    audioStreamConnector.changeToBridge();
                }
            } else if (str.startsWith(Xyz.COMMAND_AUDIO_CREATE_ROOM)) {
                ConnectorHandler audioStreamConnector = findAudioStreamConnector(handler);
                if (audioStreamConnector != null) {
                    // 随机创建聊天室
                    AudioRoom room = createNewRoom();
                    // 加入一个客户端
                    joinRoom(room, audioStreamConnector);
                    //
                    sendMessageToClient(handler, Xyz.COMMAND_INFO_AUDIO_ROOM + room.getRoomCode());
                }
            } else if (str.startsWith(Xyz.COMMAND_AUDIO_LEAVE_ROOM)) {
                // 离开房间命令
                ConnectorHandler audioStreamConnector = findAudioStreamConnector(handler);
                if (audioStreamConnector != null) {
                    // 任意一个人离开都销毁房间
                    dissolveRoom(audioStreamConnector);
                    // 发送离开消息
                    sendMessageToClient(handler, Xyz.COMMAND_INFO_AUDIO_STOP);
                }
            } else if (str.startsWith(Xyz.COMMAND_AUDIO_JOIN_ROOM)) {
                // 加入房间操作
                ConnectorHandler audioStreamConnector = findAudioStreamConnector(handler);
                if (audioStreamConnector != null) {
                    // 取得房间号
                    String roomCode = str.substring(Xyz.COMMAND_AUDIO_JOIN_ROOM.length());
                    AudioRoom room = audioRoomMap.get(roomCode);
                    if (room != null && joinRoom(room, audioStreamConnector)) {
                        // 另一个链接
                        ConnectorHandler otherHandler = room.getOtherHandler(audioStreamConnector);

                        // 相互搭建好桥接
                        otherHandler.bingToBridge(audioStreamConnector.getSender());
                        audioStreamConnector.bingToBridge(otherHandler.getSender());

                        // 成功加入房间
                        sendMessageToClient(handler, Xyz.COMMAND_INFO_AUDIO_START);
                        // 给对方发送可开始聊天的消息
                        sendStreamConnectorMessage(otherHandler, Xyz.COMMAND_INFO_AUDIO_START);
                    } else {
                        // 房间没找到，房间人员已满
                        sendMessageToClient(handler, Xyz.COMMAND_INFO_AUDIO_ERROR);
                    }
                }
            } else {
                return false;
            }
            return true;
        }
    }

    /**
     * 链接关闭时退出音频房间等操作
     */
    private class RemoveAudioQueueOnConnectorClosedChain extends ConnectorCloseChain {

        @Override
        protected boolean consume(ConnectorHandler handler, Connector connector) {
            if (audioCmdToStreamMap.containsKey(handler)) {
                // cmd连接断开
                audioCmdToStreamMap.remove(handler);
            } else if (audioStreamToCmdMap.containsKey(handler)) {
                // 流断开
                audioStreamToCmdMap.remove(handler);
                // 解散房间
                dissolveRoom(handler);
            }
            return false;
        }
    }
}

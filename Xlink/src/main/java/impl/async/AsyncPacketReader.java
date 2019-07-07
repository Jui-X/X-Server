package impl.async;

import core.Frame;
import core.IOParameter;
import core.SendPacket;
import core.datastructure.BytePriorityNode;
import core.frames.AbsSendPacketFrame;
import core.frames.CancelSendFrame;
import core.frames.SendEntityFrame;
import core.frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:44
 **/
public class AsyncPacketReader implements Closeable {
    private final PacketProvider provider;
    private IOParameter parameter = new IOParameter();

    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    private short lastIdentifier;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    synchronized void cancel(SendPacket packet) {
        if (nodeSize == 0) {
            return;
        }

        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            // 首帧，并且未被发送任何数据，直接取消后不需要添加发送帧
                            break;
                        }
                    }

                    // 添加终止帧，通知到接收方
                    CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getFrameIdentifier());
                    appendNewFrame(cancelSendFrame);

                    // 异常终止，返回失败
                    provider.completeSendPacket(packet, false);

                    break;
                }
            }
        }
    }

    /**
     * 请求从 {@link #provider}队列中拿一份Packet进行发送
     * @return 如果当前Reader中有可以用于网络发送的数据，则返回True
     */
    boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }

        SendPacket packet = provider.takePacket();

        if (packet != null) {
            short identifier = generateIdentifier();
            SendHeaderFrame headerFrame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(headerFrame);
        }

        synchronized (this) {
            return nodeSize != 0;
        }
    }

    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (newNode != null) {
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    private synchronized Frame getCurrentFrame() {
        if (node != null) {
            return node.item;
        }
        return null;
    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            node = removeNode.next;
        } else {
            before.next = removeNode.next;
        }
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    IOParameter fillWithData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }

        try {
            if (currentFrame.handle(parameter)) {
                // 消费此帧成功
                // 基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    // 如果此帧是末尾实体帧
                    // 则回调通知外层发送已完成
                    provider.completeSendPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }

                // 从链表头部弹出
                popCurrentFrame();
            }
            return parameter;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    /**
     * 关闭当前Reader，关闭时应关闭所有的Frame对应的Packet
     */
    @Override
    public synchronized void close() {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completeSendPacket(packet, false);
            }
            // 修复bug1：Frame所在链表中无限循环头部节点
            node = node.next;
        }

        nodeSize = 0;
        node = null;
    }

    /**
     * Packet提供者
     */
    interface PacketProvider {
        /**
         * 拿Packet操作
         * @return 如果队列有可以发送的Packet则返回不为null
         */
        SendPacket takePacket();

        /**
         * 结束一份Packet
         * @param packet    发送包
         * @param isSucceed 是否成功发送完成
         */
        void completeSendPacket(SendPacket packet, boolean isSucceed);
    }
}

package impl.async;

import core.IOParameter;
import core.ReceivePacket;
import core.Frame;
import core.frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:44
 **/
public class AsyncPacketWriter implements Closeable {
    private final PacketProvider provider;
    private final HashMap<Short, PacketInfo> packetMap = new HashMap<>();
    private final IOParameter parameter = new IOParameter();
    private volatile Frame tempFrame;

    public AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 构建一份数据容纳封装
     * 当前帧如果没有则返回至少6字节长度的IoArgs，
     * 如果当前帧有，则返回当前帧未消费完成的区间
     * @return IoParameter
     */
    IOParameter takeIOParameter() {
        parameter.setLimit(tempFrame == null ? Frame.FRAME_HEADER_LENGTH : tempFrame.getConsumableLength());
        return parameter;
    }

    /**
     * 消费IoArgs中的数据
     *
     * @param parameter IOParamter
     */
    void consumeIOParameter(IOParameter parameter) {
        if (tempFrame == null) {
            Frame temp;
            do {
                // 还有未消费数据，则重复构建帧
                temp = buildNewFrame(parameter);
            } while (temp == null && parameter.remained());

            if (temp == null) {
                // 最终消费数据完成，但没有可消费区间，则直接返回
                return;
            }

            tempFrame = temp;
            if (!parameter.remained()) {
                // 没有数据，则直接返回
                return;
            }

            // 确保此时currentFrame一定不为null
            Frame currentFrame = tempFrame;
            do {
                try {
                    if (currentFrame.handle(parameter)) {
                        // 某帧已接收完成
                        if (currentFrame instanceof ReceiveHeaderFrame) {
                            // Packet 头帧消费完成，则根据头帧信息构建接收的Packet
                            ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                            ReceivePacket packet = provider.takePacket(headerFrame.getType(), headerFrame.getPacketLength(),
                                    headerFrame.getPacketHeaderInfo());
                            appendPacket(headerFrame.getFrameIdentifier(), packet);
                        } else if (currentFrame instanceof ReceiveEntityFrame) {
                            // Packet 实体帧消费完成，则将当前帧消费到Packet
                            completeReceiveEntityFrame((ReceiveEntityFrame) currentFrame);
                        }

                        // 接收完成后，直接退出循环，如果还有未消费数据则交给外层调度
                        tempFrame = null;
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (parameter.remained());
        }
    }

    /**
     * 当某Packet实体帧消费完成时调用
     * @param frame 帧信息
     */
    private void completeReceiveEntityFrame(ReceiveEntityFrame frame) {
        synchronized (packetMap) {
            short identifier = frame.getFrameIdentifier();
            int length = frame.getFrameBodyLength();
            PacketInfo packetInfo = packetMap.get(identifier);
            packetInfo.unReceivedLength = length;
            if (packetInfo.unReceivedLength <= 0) {
                provider.completeReceivePacket(packetInfo.packet, true);
                packetMap.remove(identifier);
            }
        }
    }

    /**
     * 添加一个新的Packet到当前缓冲区
     *
     * @param identifier Packet标志
     * @param packet     Packet
     */
    private void appendPacket(short identifier, ReceivePacket packet) {
        synchronized (packetMap) {
            PacketInfo packetInfo = new PacketInfo(packet);
            packetMap.put(identifier, packetInfo);
        }
    }

    /**
     * 根据args创建新的帧
     * 若当前解析的帧是取消帧，则直接进行取消操作，并返回null
     * @param parameter IOParameter
     * @return 返回新的帧
     */
    private Frame buildNewFrame(IOParameter parameter) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(parameter);
        if (frame instanceof CancelReceiveFrame) {
            cancelReceivePacket(frame.getFrameIdentifier());
            return null;
        } else if (frame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(frame.getFrameIdentifier());
            ((ReceiveEntityFrame) frame).bindPacketChannel(channel);
        }

        return frame;
    }

    /**
     * 获取Packet对应的输出通道，用以设置给帧进行数据传输
     * 因为关闭当前map的原因，可能存在返回NULL=
     * @param identifier Packet对应的标志
     * @return 通道
     */
    private WritableByteChannel getPacketChannel(short identifier) {
        synchronized (packetMap) {
            PacketInfo packetInfo = packetMap.get(identifier);

            return packetInfo == null ? null : packetInfo.channel;
        }
    }

    /**
     * 取消某Packet继续接收数据
     * @param identifier Packet标志
     */
    private void cancelReceivePacket(short identifier) {
        synchronized (packetMap) {
            PacketInfo packetInfo = packetMap.get(identifier);
            if (packetInfo != null) {
                ReceivePacket packet = packetInfo.packet;
                provider.completeReceivePacket(packet, false);
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        synchronized (packetMap) {
            Collection<PacketInfo> packets = packetMap.values();
            for (PacketInfo info : packets) {
                provider.completeReceivePacket(info.packet, false);
            }
            packetMap.clear();
        }
    }

    /**
     * Packet提供者
     */
    interface PacketProvider {
        /**
         * 拿Packet操作
         * @param type       Packet类型
         * @param length     Packet长度
         * @param headerInfo Packet headerInfo
         * @return 通过类型，长度，描述等信息得到一份接收Packet
         */
        ReceivePacket takePacket(byte type, long length, byte[] headerInfo);

        /**
         * 结束一份Packet
         * @param packet    接收包
         * @param isSucceed 是否成功接收完成
         */
        void completeReceivePacket(ReceivePacket packet, boolean isSucceed);
    }

    static class PacketInfo {
        final ReceivePacket packet;
        final WritableByteChannel channel;
        volatile long unReceivedLength;


        PacketInfo(ReceivePacket<?,?> packet) {
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unReceivedLength = packet.length();
        }
    }
}

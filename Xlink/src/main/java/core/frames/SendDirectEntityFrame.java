package core.frames;

import core.Frame;
import core.IOParameter;
import core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-09 17:05
 **/
public class SendDirectEntityFrame extends AbsSendPacketFrame{
    private final ReadableByteChannel channel;

    public SendDirectEntityFrame(short identifier, int available, SendPacket packet, ReadableByteChannel channel) {
        super(Math.min(available, Frame.MAX_CAPACITY), Frame.TYPE_PACKET_ENTITY, Frame.FLAG, identifier, packet);
        this.channel = channel;
    }

    /**
     * 通过Packet构建内容发送帧
     * 若当前内容无可读内容，则直接发送取消帧
     *
     * @param packet     Packet
     * @param identifier 当前标识
     * @return 内容帧
     */
    public static Frame buildEntityFrame(SendPacket<?> packet, short identifier) {
        int available = packet.available();
        if (available <= 0) {
            // 直流结束
            // TODO 可用专门的断点帧替代，表示Packet已断
            return new CancelSendFrame(identifier);
        }

        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);
        return new SendDirectEntityFrame(identifier, available, packet, channel);
    }

    @Override
    protected Frame buildNextFrame() {
        // 直流类型
        int available = packet.available();
        // 无数据可输出则直流结束
        if (available <= 0) {
            return new CancelSendFrame(getFrameIdentifier());
        }
        // 下一帧
        return new SendDirectEntityFrame(getFrameIdentifier(), available, packet, channel);
    }

    @Override
    protected int consumeBody(IOParameter parameter) throws IOException {
        if (packet == null) {
            // 已终止当前帧，则返回假数据
            return parameter.fillEmpty(bodyRemaining);
        }
        return parameter.readFrom(channel);
    }
}

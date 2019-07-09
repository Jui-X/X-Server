package core.frames;

import core.Frame;
import core.IOParameter;
import core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:45
 **/
public class SendEntityFrame extends AbsSendPacketFrame {
    private final ReadableByteChannel channel;
    private final long unConsumeEntityLength;

    SendEntityFrame(long entityLength, short identifier, ReadableByteChannel channel, SendPacket packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY), Frame.TYPE_PACKET_ENTITY, Frame.FLAG, identifier, packet);
        unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IOParameter parameter) throws IOException {
        if (packet == null) {
            // 已终止发送当前帧，填充假数据
            return parameter.fillEmpty(bodyRemaining);
        }
        return parameter.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }
        // 发送下一帧
        return new SendEntityFrame(unConsumeEntityLength, getFrameIdentifier(), channel, packet);
    }
}

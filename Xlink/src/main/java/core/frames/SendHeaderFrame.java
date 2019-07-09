package core.frames;

import core.Frame;
import core.IOParameter;
import core.Packet;
import core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:45
 **/
public class SendHeaderFrame extends AbsSendPacketFrame {
    static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;
    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket packet) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH, Frame.TYPE_PACKET_HEADER, Frame.FLAG, identifier, packet);

        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();

        body = new byte[bodyRemaining];
        // 帧体长度
        body[0] = (byte) (packetLength >> 32);
        body[1] = (byte) (packetLength >> 24);
        body[2] = (byte) (packetLength >> 16);
        body[3] = (byte) (packetLength >> 8);
        body[4] = (byte) packetLength;

        body[5] = packetType;

        if (packetHeaderInfo != null) {
            System.arraycopy(packetHeaderInfo, 0, body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }

    @Override
    protected int consumeBody(IOParameter parameter) throws IOException {
        int count = bodyRemaining;
        int offset = body.length - count;
        return parameter.readFrom(body, offset, count);
    }

    @Override
    public Frame buildNextFrame() {
        byte type = packet.type();
        if (type == Packet.TYPE_STREAM_DIRECT) {
            // 直流类型
            return SendDirectEntityFrame.buildEntityFrame(packet, getFrameIdentifier());
        } else {
            // 普通数据类型
            InputStream stream = packet.open();
            ReadableByteChannel channel = Channels.newChannel(stream);
            return new SendEntityFrame(packet.length(), getFrameIdentifier(), channel, packet);
        }
    }
}

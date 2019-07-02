package core.frames;

import core.IOParameter;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:46
 **/
public class ReceiveHeaderFrame extends AbsReceiveFrame {
    // Packet头部存放整个包的数据长度以及其他相关信息的地方
    private final byte[] body;

    public ReceiveHeaderFrame(byte[] header) {
        super(header);
        this.body = new byte[bodyRemaining];
    }

    @Override
    protected int consumeBody(IOParameter parameter) {
        int offset = body.length - bodyRemaining;
        return parameter.writeTo(body, offset);
    }

    public long getPacketLength() {
        return ((((long) body[0]) & 0xFFL) << 32) |
                ((((long) body[1]) & 0xFFL) << 24) |
                ((((long) body[2]) & 0xFFL) << 16) |
                ((((long) body[3]) & 0xFFL) << 8) |
                (((long) body[4]) & 0xFFL);
    }

    public byte getType() {
        return body[5];
    }

    public byte[] getPacketHeaderInfo() {
        if (body.length > FRAME_HEADER_LENGTH) {
            byte[] headerInfo = new byte[body.length - FRAME_HEADER_LENGTH];
            System.arraycopy(body, SendHeaderFrame.PACKET_HEADER_FRAME_MIN_LENGTH, headerInfo, 0, headerInfo.length);
            return headerInfo;
        }
        return null;
    }
}

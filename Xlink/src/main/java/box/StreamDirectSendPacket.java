package box;

import core.Packet;
import core.SendPacket;

import java.io.InputStream;

/**
 * @param: none
 * @description: 直流发送Packet
 * @author: KingJ
 * @create: 2019-07-09 16:44
 **/
public class StreamDirectSendPacket extends SendPacket<InputStream> {
    private InputStream inputStream;

    public StreamDirectSendPacket(InputStream inputStream) {
        // 用以读取数据进行输出的输入流
        this.inputStream = inputStream;
        // 长度不固定，所以为最大值
        this.length = MAX_PACKET_SIZE;
    }

    @Override
    protected InputStream createStream() {
        return inputStream;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }
}

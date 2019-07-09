package box;

import core.Packet;
import core.ReceivePacket;

import java.io.OutputStream;

/**
 * @param: none
 * @description: 直流接收Packet
 * @author: KingJ
 * @create: 2019-07-09 16:49
 **/
public class StreamDirectReceivePacket extends ReceivePacket<OutputStream, OutputStream> {
    private OutputStream outputStream;

    public StreamDirectReceivePacket(long len, OutputStream outputStream) {
        super(len);
        // 用以读取数据进行输出的输出流
        this.outputStream = outputStream;
    }

    @Override
    protected OutputStream buildEntity(OutputStream stream) {
        return outputStream;
    }

    @Override
    protected OutputStream createStream() {
        return outputStream;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }
}

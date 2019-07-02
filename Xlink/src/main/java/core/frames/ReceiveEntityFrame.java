package core.frames;

import core.IOParameter;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:45
 **/
public class ReceiveEntityFrame extends AbsReceiveFrame{
    private WritableByteChannel channel;

    public ReceiveEntityFrame(byte[] header) {
        super(header);
    }

    public void bindPacketChannel(WritableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IOParameter parameter) throws IOException {
        return channel == null ? parameter.setEmpty(bodyRemaining) : parameter.writeTo(channel);
    }
}

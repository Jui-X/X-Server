package box;

import core.SendPacket;

import java.io.ByteArrayInputStream;

public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {
    private final byte[] bytes;

    public BytesSendPacket(byte[] bytes) {
        this.bytes = bytes;
        this.length = bytes.length;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }
}

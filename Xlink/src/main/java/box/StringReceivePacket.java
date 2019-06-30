package box;

import java.io.ByteArrayOutputStream;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 17:02
 **/
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {
    private String msg;

    public StringReceivePacket(long len) {
        super(len);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }
}

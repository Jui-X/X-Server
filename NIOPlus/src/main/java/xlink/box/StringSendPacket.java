package xlink.box;

import xlink.core.SendPacket;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 17:00
 **/
public class StringSendPacket extends SendPacket {
    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public void close() {

    }
}

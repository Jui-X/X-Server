package core;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 16:57
 **/
public abstract class ReceivePacket extends Packet {
    public abstract void save(byte[] bytes, int size);
}

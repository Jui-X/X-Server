package core;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 16:55
 **/
public abstract class SendPacket extends Packet {
    public abstract byte[] bytes();
    public boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }
}

package core;

import java.io.InputStream;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 16:55
 **/
public abstract class SendPacket<T extends InputStream> extends Packet<T> {
    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * 设置取消发送标记
     */
    public void cancel() {
        isCanceled = true;
    }
}

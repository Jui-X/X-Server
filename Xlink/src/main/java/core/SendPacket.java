package core;

import java.io.IOException;
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

    /**
     * 获取当前可用数据大小
     * PS: 对于流的类型有限制，文件流一般可用正常获取，
     * 对于正在填充的流不一定有效，或得不到准确值
     * <p>
     * 我们利用该方法不断得到直流传输的可发送数据量，从而不断生成Frame
     * <p>
     * 缺陷：对于流的数据量大于Int有效值范围外则得不到准确值
     * <p>
     * 一般情况下，发送数据包时不使用该方法，而使用总长度进行运算
     * 对于直流传输则需要使用该方法，因为对于直流而言没有最大长度
     *
     * @return 默认返回stream的可用数据量：0代表无数据可输出了
     */
    public int available() {
        InputStream stream = open();
        try {
            int available = stream.available();
            if (available < 0) {
                return 0;
            }
            return available;
        } catch (IOException e) {
            // 在Packet层面即抛出异常
            // 只告诉Frame层当前available是否大于0，需不需要生成帧
            return 0;
        }
    }
}

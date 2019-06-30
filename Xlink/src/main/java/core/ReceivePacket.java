package core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 16:57
 **/
public abstract class ReceivePacket<Stream extends OutputStream, Entity> extends Packet<Stream> {
    // 定义当前接收包的最终实例
    private Entity entity;

    public ReceivePacket(long len) {
        this.length = len;
    }

    /**
     * 得到最终接收到的数据实体
     * @return 数据实体
     */
    public Entity entity() {
        return entity;
    }

    /**
     * 先关闭流，随后将流的内容转化为对应的实体
     * @param stream
     * @return
     */
    protected abstract Entity buildEntity(Stream stream);

    @Override
    protected void closeStream(Stream stream) throws IOException {
        super.closeStream(stream);
        entity = buildEntity(stream);
    }
}

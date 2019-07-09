package core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @param: none
 * @description: 公共数据的封装
 *               提供了类型以及基本长度的定义
 * @author: KingJ
 * @create: 2019-06-28 16:50
 **/
public abstract class Packet<Stream extends Closeable> implements Closeable {
    /**
     * 最大包大小，5个字节满载组成的Long类型
     */
    public static final long MAX_PACKET_SIZE = ((0XFFL) << 32) | ((0XFFL) << 24) | ((0XFFL << 16)) | ((0XFFL << 8)) | (0XFFL);
    // 数据类型
    // BYTES类型
    public static final byte TYPE_MEMORY_BYTES = 1;
    // String类型
    public static final byte TYPE_MEMORY_STRING = 2;
    // File类型
    public static final byte TYPE_STREAM_FILE = 3;
    // 长链接流类型
    public static final byte TYPE_STREAM_DIRECT = 4;
    // 数据长度
    protected long length;
    // 流
    private Stream stream;

    public long length() {
        return length;
    }

    public final Stream open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }

    /**
     * 创建流操作，应当将当前需要传输的数据转化为流
     * @return {@link java.io.InputStream} or {@link java.io.OutputStream}
     */
    protected abstract Stream createStream();

    /**
     * 对外的关闭资源操作，如果流处于打开状态应当进行关闭
     * @throws IOException IO异常
     */
    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    /**
     * 类型，通过子类具体实现得到
     * @return
     */
    public abstract byte type();

    /**
     * 头部额外信息，用于携带额外的校验信息等
     * @return byte 数组，最大255长度
     */
    public byte[] headerInfo() {
        return null;
    }
}

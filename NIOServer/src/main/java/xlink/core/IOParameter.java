package xlink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @param: none
 * @description: 读写数据时的数据封装类
 * @author: KingJ
 * @create: 2019-06-02 13:33
 **/
public class IOParameter {
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    public String bufferToString() {
        // 把当前buffer中的数据读取出来转换成String并丢弃换行符
        return new String(buffer.array(), 0, buffer.position() -1);
    }

    /**
     *
     * 监听当前IOParameter状态
     *
     **/
    public interface IOParaEventListener {
        void onStart(IOParameter parameter);

        void onComplete(IOParameter parameter);
    }
}

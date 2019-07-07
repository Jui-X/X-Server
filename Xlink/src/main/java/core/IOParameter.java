package core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-02 13:33
 **/
public class IOParameter {
    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    /**
     * 从bytes数组中读取数据进行消费
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到bytes数组中
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }
    /**
     * 从channel中读取数据
     * @return
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        int getBytes = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            getBytes += len;
        }

        return getBytes;
    }

    /**
     * 写入数据到channel中
     * @return
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 从SocketChannel中读取数据
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;

        // 读取或写数据到Socket原理
        // 回调当前可读、可写时我们进行数据填充或者消费
        // 但是过程中可能SocketChannel资源被其他SocketChannel占用了资源
        // 那么我们应该让出当前的线程调度，让应该得到数据消费的SocketChannel的到CPU调度
        // 而不应该单纯的buffer.hasRemaining()判断
        do {
            len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException("Cannot read any data with " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);

        return bytesProduced;
    }

    /**
     * 写入数据到SocketChannel
     * @param channel
     * @return
     * @throws IOException
     */
    public int writeTo(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;

        do {
            len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException("Cannot write any data with " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);

        return bytesProduced;
    }

    /**
     * 开始写入数据到IOParameter
     */
    public void startWriting() {
        buffer.clear();
        buffer.limit(limit);
    }

    /**
     * 写完数据后调用
     */
    public void finishWriting() {
        buffer.flip();
    }

    /**
     * 设置单次写操作的容纳区间
     * @param limit 区间大小
     */
    public void setLimit(int limit) {
        this.limit = Math.min(limit, buffer.capacity());
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 填充数据
     * @param size 想要填充数据的长度
     * @return 真实填充数据的长度
     */
    public int fillWithEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * 清空部分数据
     *
     * @param size 想要清空的数据长度
     * @return 真实清空的数据长度
     */
    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }

    /**
     * 数据的生产者或消费者
     */
    public interface IOParaEventProcessor {
        /**
         * 提供一份可消费的IOParameter
         * @return
         */
        IOParameter provideParameter();

        /**
         * 消费失败时回调
         * @param parameter parameter
         * @param e         异常信息
         */
        void onConsumeFailed(IOParameter parameter, Exception e);

        /**
         * 消费完成时的回调
         * @param parameter parameter
         */
        void onConsumeCompleted(IOParameter parameter);
    }
}

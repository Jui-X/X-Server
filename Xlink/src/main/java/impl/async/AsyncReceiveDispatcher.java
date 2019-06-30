package impl.async;

import Utils.CloseUtils;
import core.*;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 22:09
 **/
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IOParameter.IOParaEventProcessor {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 接收者
    private final Receiver receiver;
    // 接收回调
    private final ReceivePacketCallback callback;

    // 接收数据
    private IOParameter parameter = new IOParameter();
    // 当前正在接受的packet
    private ReceivePacket<?, ?> receivePacket;
    //
    private WritableByteChannel writeChannel;
    // 读取数据的大小
    private long size;
    // 当前读取位置
    private long position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.callback = callback;
    }

    @Override
    public void start() {
        // 外层调用start方法时首先调用开始接收操作
        registerReceive();
    }

    private void registerReceive() {
        try {
            // 将listener传入进去
            // 在接受完成的进行回调
            // 执行onComplete方法 打印（receiveNewMessage） 并 继续接收数据（readNextMsg）
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            completeReceivePacket(false);
        }
    }

    /**
     * 解析数据到Packet
     * @param parameter
     */
    private void assemblePacket(IOParameter parameter) {
        if (receivePacket == null) {
            // 读取报文长度
            int length = parameter.readLength();
            // 根据文件长度暂时判断是文件类型还是String类型
            byte type = length > 200 ? Packet.TYPE_STREAM_FILE : Packet.TYPE_MEMORY_STRING;

            receivePacket = callback.onNewPacketArrived(type, length);
            writeChannel = Channels.newChannel(receivePacket.open());
            size = length;
            position = 0;
        }

        try {
            int len = parameter.writeTo(writeChannel);

            position += len;

            if (position == size) {
                // 检查是否完成一份Packet的接收
                completeReceivePacket(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            completeReceivePacket(false);
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completeReceivePacket(boolean isSucceed) {
        ReceivePacket packet = this.receivePacket;
        CloseUtils.close(packet);
        receivePacket = null;

        WritableByteChannel channel = this.writeChannel;
        CloseUtils.close(channel);
        writeChannel = null;

        if (packet != null) {
            // 调用callback函数告诉外层有新的数据接收完成
            callback.onReceivePacketCompleted(packet);
        }
    }

    @Override
    public IOParameter provideParameter() {
        IOParameter ioParameter = parameter;

        int receiveSize;
        if (receivePacket == null) {
            // 最小长度为4，一个int的长度
            receiveSize = 4;
        } else {
            receiveSize = (int) Math.min(size - position, parameter.capacity());
        }
        // 设置本次接收大小
        parameter.setLimit(receiveSize);

        return ioParameter;
    }

    @Override
    public void onConsumeFailed(IOParameter parameter, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IOParameter parameter) {
        assemblePacket(parameter);
        // 接收下一条数据
        registerReceive();
    }
}

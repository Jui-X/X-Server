package impl.async;

import Utils.CloseUtils;
import box.StringReceivePacket;
import core.IOParameter;
import core.ReceiveDispatcher;
import core.ReceivePacket;
import core.Receiver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 22:09
 **/
public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 接收者
    private final Receiver receiver;
    // 接收回调
    private final ReceivePacketCallback callback;

    // 接收数据
    private IOParameter parameter = new IOParameter();
    // 当前正在接受的packet
    private ReceivePacket receivePacket;
    // 每次接收的数据都要写到buffer中，然后再写到packet中去
    private byte[] buffer;
    // 读取数据的大小
    private int size;
    // 当前读取位置
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(listener);
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
            receiver.receiveAsync(parameter);
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
            ReceivePacket packet = this.receivePacket;
            if (packet != null) {
                receivePacket = null;
                CloseUtils.close(packet);
            }
        }
    }

    private IOParameter.IOParaEventListener listener = new IOParameter.IOParaEventListener() {
        @Override
        public void onStart(IOParameter parameter) {
            int receiveSize;
            if (receivePacket == null) {
                // 最小长度为4，一个int的长度
                receiveSize = 4;
            } else {
                receiveSize = Math.min(size - position, parameter.capacity());
            }
            parameter.setLimit(receiveSize);
        }

        @Override
        public void onComplete(IOParameter parameter) {
            assemblePacket(parameter);
            // 解析完成后，接收下一条数据
            registerReceive();
        }
    };

    /**
     * 解析数据到Packet
     * @param parameter
     */
    private void assemblePacket(IOParameter parameter) {
        if (receivePacket == null) {
            // 读取报文长度
            int length = parameter.readLength();
            receivePacket = new StringReceivePacket(length);
            buffer = new byte[length];
            size = length;
            position = 0;
        }

        int len = parameter.writeTo(buffer, 0);
        if (len > 0) {
            receivePacket.save(buffer, len);
            position += len;

            if (position == size) {
                // 检查是否完成一份Packet的接收
                completeReceivePacket();
                receivePacket = null;
            }
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completeReceivePacket() {
        ReceivePacket packet = this.receivePacket;
        CloseUtils.close(packet);
        // 调用callback函数告诉外层有新的数据接收完成
        callback.onReceivePacketCompleted(packet);
    }
}

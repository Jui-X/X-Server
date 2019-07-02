package impl.async;

import Utils.CloseUtils;
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
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IOParameter.IOParaEventProcessor,
        AsyncPacketWriter.PacketProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 接收者
    private final Receiver receiver;
    // 接收回调
    private final ReceivePacketCallback callback;

    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

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
            writer.close();
        }
    }

    @Override
    public IOParameter provideParameter() {
        return writer.takeIOParameter();
    }

    @Override
    public void onConsumeFailed(IOParameter parameter, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IOParameter parameter) {
        do {
            writer.consumeIOParameter(parameter);
        } while (parameter.remained());

        // 接收下一条数据
        registerReceive();
    }

    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return callback.onNewPacketArrived(type, length);
    }

    @Override
    public void completeReceivePacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }
}

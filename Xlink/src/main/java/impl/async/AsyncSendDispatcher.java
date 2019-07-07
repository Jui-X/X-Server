package impl.async;

import Utils.CloseUtils;
import core.IOParameter;
import core.SendDispatcher;
import core.SendPacket;
import core.Sender;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-06-28 20:09
 **/
public class AsyncSendDispatcher implements SendDispatcher, IOParameter.IOParaEventProcessor,
        AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private AsyncPacketReader reader = new AsyncPacketReader(this);

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendProcessor(this);
    }

    /**
     * 发送Packet
     * 首先添加到队列，如果当前状态为未启动发送状态
     * 则，尝试让reader提取一份packet进行数据发送
     * 如果提取数据后reader有数据，则进行异步输出注册
     * @param packet 数据
     */
    @Override
    public void send(SendPacket packet) {
        // 将需要发送的packet存入队列
        queue.offer(packet);
        requestSend();
    }

    /**
     * reader从当前队列中提取一份Packet
     * @return 如果队列有可用于发送的数据则返回该Packet
     */
    @Override
    public SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet == null) {
            return null;
        }
        // packet不等于空同时被取消
        if (packet != null && packet.isCanceled()) {
            // 已取消，不用发送
            // 取出下一个Packet
            return takePacket();
        }
        return packet;
    }

    /**
     * 完成Packet发送
     * @param isSucceed 是否成功
     */
    @Override
    public void completeSendPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }

            if (reader.requestTakePacket()) {
                try {
                    // 如果position < size，则继续发送
                    // 把数据放入parameter中
                    // 再进行注册监听，当可以发送的时候就发送数据，并回调发送完成的状态
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() {
        if (isSending.compareAndSet(false, true)) {
            // reader调用关闭
            reader.close();
            // 清空队列防止内存泄漏
            queue.clear();
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    /**
     * 取消Packet操作
     * 如果还在队列中，代表Packet未进行发送，则直接标志取消，并返回即可
     * 如果未在队列中，则让reader尝试扫描当前发送序列，查询是否当前Packet正在发送
     * 如果是则进行取消相关操作
     * @param packet 数据
     */
    @Override
    public void cancel(SendPacket packet) {
        boolean ret = queue.remove(packet);
        // 返回true表示此Packet已从队列中移除
        if (ret) {
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    /**
     * 网络发送就绪回调，当前已进入发送就绪状态，等待填充数据进行发送
     * 此时从reader中填充数据，并进行后续网络发送
     * @return NULL，可能填充异常，或者想要取消本次发送
     */
    @Override
    public IOParameter provideParameter() {
        return isClosed.get() ? null : reader.fillWithData();
    }

    /**
     * 网络发送IoArgs出现异常
     *
     * @param parameter parameter
     * @param e         异常信息
     */
    @Override
    public void onConsumeFailed(IOParameter parameter, Exception e) {
        e.printStackTrace();
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前的数据
        requestSend();
    }

    /**
     * 完成Packet发送
     */
    @Override
    public void onConsumeCompleted(IOParameter parameter) {
        // 设置发送当前状态
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前的数据
        requestSend();
    }
}

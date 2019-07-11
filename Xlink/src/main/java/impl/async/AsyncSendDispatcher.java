package impl.async;

import Utils.CloseUtils;
import core.IOParameter;
import core.SendDispatcher;
import core.SendPacket;
import core.Sender;
import impl.exceptions.EmptyIOParameterException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
    private final BlockingQueue<SendPacket> queue = new ArrayBlockingQueue<>(16);
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
     *
     * @param packet 数据
     */
    @Override
    public void send(SendPacket packet) {
        // 将需要发送的packet存入队列
        try {
            queue.put(packet);
            requestSend(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送心跳帧，将心跳帧放到帧发送队列进行发送
     */
    @Override
    public void sendHeartbeat() {
        if (!queue.isEmpty()) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()) {
            requestSend(false);
        }
    }

    /**
     * reader从当前队列中提取一份Packet
     *
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
     *
     * @param isSucceed 是否成功
     */
    @Override
    public void completeSendPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend(boolean callFromIOConsume) {
        synchronized (isSending) {
            final AtomicBoolean isRegisterSending = this.isSending;
            final boolean oldState = isRegisterSending.get();
            // 表示当前调用是否来普通消费流程
            // 1.通道已关闭
            // 2.已被注册发送且不是来自于消费流程（心跳包或业务层发送Packet
            if (isClosed.get() || (oldState && !callFromIOConsume)) {
                return;
            }

            if (callFromIOConsume && !oldState) {
                throw new IllegalStateException("");
            }

            // 返回True代表有数据需要发送
            if (reader.requestTakePacket()) {
                try {
                    isRegisterSending.set(true);
                    // 如果position < size，则继续发送
                    // 把数据放入parameter中
                    // 再进行注册监听，当可以发送的时候就发送数据，并回调发送完成的状态
                    sender.postSendAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                    CloseUtils.close(this);
                }
            } else {
                isRegisterSending.set(false);
            }
        }
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
     *
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
     *
     * @return NULL，可能填充异常，或者想要取消本次发送
     */
    @Override
    public IOParameter provideParameter() {
        return isClosed.get() ? null : reader.fillWithData();
    }

    /**
     * 网络发送IoArgs出现异常
     *
     * @param e 异常信息
     * @return 是否需要关闭当前链接
     */
    @Override
    public boolean onConsumeFailed(Throwable e) {
        if (e instanceof EmptyIOParameterException) {
            // 继续请求发送当前的数据
            requestSend(true);
            return false;
        } else {
            CloseUtils.close(this);
            return true;
        }
    }

    /**
     * 完成Packet发送
     * 网络发送IOParameter完成回调
     * 在该方法进行reader对当前队列Packet的提取，并进行后续的数据发送注册
     */
    @Override
    public boolean onConsumeCompleted(IOParameter parameter) {
        // 设置发送当前状态
        synchronized (isSending) {
            // 是否已经注册发送
            AtomicBoolean isRegisterSending = this.isSending;
            // 是否处于运行态，即没有被关闭
            final boolean isRunning = !isClosed.get();

            // 发送过程的回调则证明已注册发送
            if (!isRegisterSending.get() && isRunning) {
                throw new IllegalStateException("");
            }

            isRegisterSending.set(isRunning && reader.requestTakePacket());

            return isRegisterSending.get();
        }
    }
}

package core;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-10 17:04
 **/

import core.IOProvider;

import java.nio.channels.SocketChannel;

/**
 * 可用以进行调度的任务封装
 * 任务执行的回调、当前任务类型、任务对应的通道
 */
public abstract class IOTask {
    // 贯穿框架
    public final SocketChannel channel;
    // 注册什么样的任务
    public final int ops;

    public IOTask(SocketChannel channel, int ops) {
        this.channel = channel;
        this.ops = ops;
    }

    public abstract boolean processIO();

    public abstract void fireThrowable(Throwable e);
}

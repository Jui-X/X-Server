package core.frames;

import core.Frame;
import core.IOParameter;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:46
 **/
public abstract class AbsSendFrame extends Frame {
    // 帧头可读写区域大小
    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    // 帧体可读写区域大小
    volatile int bodyRemaining;

    /**
     * @param length     帧体的长度
     * @param type       当前帧类型
     * @param flag       帧的加密信息
     * @param identifier 帧的唯一标识
     */
    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemaining = length;
    }

    @Override
    public synchronized boolean handle(IOParameter parameter) throws IOException{
        try {
            // 可读部分的数据大小
            parameter.setLimit(headerRemaining + bodyRemaining);
            parameter.startWriting();

            if (headerRemaining > 0 && parameter.remained()) {
                headerRemaining -= consumeHeader(parameter);
            }

            if (headerRemaining == 0 && parameter.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(parameter);
            }

            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            parameter.finishWriting();
        }
    }

    /**
     * 将Header信息写入IOParameter中
     * @param parameter
     * @return
     */
    private byte consumeHeader(IOParameter parameter) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) parameter.readFrom(header, offset, count);
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    protected abstract int consumeBody(IOParameter parameter) throws IOException;

    /**
     * 是否已经处于发送数据中，如果已经发送了部分数据则返回True
     * 只要头部数据已经开始消费，则肯定已经处于发送数据中
     * @return True，已发送部分数据
     */
    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}

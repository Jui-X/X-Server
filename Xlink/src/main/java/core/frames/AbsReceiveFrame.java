package core.frames;

import core.Frame;
import core.IOParameter;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:47
 **/
public abstract class AbsReceiveFrame extends Frame {
    // 帧体可读区域大小
    volatile int bodyRemaining;

    AbsReceiveFrame(byte[] header) {
        super(header);
        bodyRemaining = getFrameBodyLength();
    }

    @Override
    public boolean handle(IOParameter parameter) throws IOException {
        if (bodyRemaining == 0) {
            return true;
        }

        bodyRemaining -= consumeBody(parameter);

        return bodyRemaining == 0;
    }

    @Override
    public int getConsumableLength() {
        return bodyRemaining;
    }

    protected abstract int consumeBody(IOParameter parameter) throws IOException;

    @Override
    public Frame nextFrame() {
        return null;
    }
}

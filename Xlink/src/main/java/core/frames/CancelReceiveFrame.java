package core.frames;

import core.IOParameter;

/**
 * 取消传输帧，接收实现
 */
public class CancelReceiveFrame extends AbsReceiveFrame {

    public CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IOParameter parameter) {
        return 0;
    }
}

package core.frames;

import core.frames.AbsReceiveFrame;

/**
 * 取消传输帧，接收实现
 */
public class CancelReceiveFrame extends AbsReceiveFrame {

    public CancelReceiveFrame(byte[] header) {
        super(header);
    }
}

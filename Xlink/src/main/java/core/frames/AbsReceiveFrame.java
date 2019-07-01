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
public class AbsReceiveFrame extends Frame {
    volatile int bodyRemaining;

    public AbsReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    public boolean handle(IOParameter parameter) throws IOException {
        return false;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}

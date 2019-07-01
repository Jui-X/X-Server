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
public class CancelSendFrame extends AbsSendFrame{
    /**
     * @param identifier 帧的唯一标识
     */
    public CancelSendFrame(short identifier) {
        super(0, TYPE_COMMAND_SEND_CANCEL, FLAG, identifier);
    }

    @Override
    protected int consumeBody(IOParameter parameter) throws IOException {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}

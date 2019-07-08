package core.frames;

import core.Frame;
import core.IOParameter;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 17:09
 **/
public class HeartbeatSendFrame extends AbsSendFrame {
    static final byte[] HEARTBEAT_DATA = new byte[] {0, 0, Frame.TYPE_COMMAND_HEARTBEAT, 0, 0, 0};

    /**
     * 通过创建固定格式的心跳包byte数组来初始化
     */
    public HeartbeatSendFrame() {
        super(HEARTBEAT_DATA);
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

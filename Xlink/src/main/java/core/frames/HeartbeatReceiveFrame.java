package core.frames;

import core.IOParameter;

import java.io.IOException;

/**
 * @param: none
 * @description: 心跳接收帧
 * @author: KingJ
 * @create: 2019-07-08 17:10
 **/
public class HeartbeatReceiveFrame extends AbsReceiveFrame {
    static final HeartbeatReceiveFrame INSTANCE = new HeartbeatReceiveFrame();

    private HeartbeatReceiveFrame() {
        super(HeartbeatSendFrame.HEARTBEAT_DATA);
    }

    @Override
    protected int consumeBody(IOParameter parameter) throws IOException {
        return 0;
    }
}

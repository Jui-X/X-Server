package core.frames;

import core.IOParameter;
import core.Frame;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-02 23:04
 **/
public class ReceiveFrameFactory {

    /**
     * 使用传入的帧头数据构建接收帧
     *
     * @param parameter IOParameter至少需要有6字节数据可读
     * @return 构建的帧头数据
     */
    public static AbsReceiveFrame createInstance(IOParameter parameter) {
        byte[] buffer = new byte[Frame.FRAME_HEADER_LENGTH];
        parameter.writeTo(buffer, 0);
        byte type = buffer[2];
        switch (type) {
            case Frame.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFrame(buffer);
            case Frame.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFrame(buffer);
            case Frame.TYPE_COMMAND_SEND_CANCEL:
                return new CancelReceiveFrame(buffer);
            case Frame.TYPE_COMMAND_HEARTBEAT:
                return HeartbeatReceiveFrame.INSTANCE;
            default:
                throw new UnsupportedOperationException("Unsupport Frame Type:" + type);
        }
    }

}

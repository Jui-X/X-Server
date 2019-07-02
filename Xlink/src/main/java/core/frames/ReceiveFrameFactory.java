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
    public static AbsReceiveFrame createInstance(IOParameter parameter) {
        byte[] buffer = new byte[Frame.FRAME_HEADER_LENGTH];
        parameter.writeTo(buffer, 0);
        byte type = buffer[2];
        switch (type) {
            case Frame.TYPE_COMMAND_SEND_CANCEL:
                return new CancelReceiveFrame(buffer);
            case Frame.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFrame(buffer);
            case Frame.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFrame(buffer);
            default:
                throw new UnsupportedOperationException("Unsupport Frame Type:" + type);
        }
    }

}

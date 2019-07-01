package core;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-01 16:34
 **/
public abstract class Frame {
    // 帧头长度
    public static final int FRAME_HEADER_LENGTH = 6;
    // 每帧最大存储长度 64KB
    public static final int MAX_CAPACITY = 64 * 1024 - 1;

    // Packet头信息帧
    public static final byte TYPE_PACKET_HEADER = 11;
    // Packet数据分片信息帧
    public static final byte TYPE_PACKET_ENTITY = 12;
    // 指令-发送取消
    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    // 指令-接受拒绝
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;
    // 指令-心跳包
    public static final byte TYPE_COMMAND_HEARTBEAT = 81;

    public static final byte FLAG = 0;

    // 帧头
    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    /**
     * @param length     帧体的长度
     * @param type       当前帧类型
     * @param flag       帧的加密信息
     * @param identifier 帧的唯一标识
     */
    public Frame(int length, byte type, byte flag, short identifier) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("");
        }
        if (identifier < 1 || identifier > 255) {
            throw new RuntimeException("");
        }

        // 帧体长度
        header[0] = (byte) (length >> 8);
        header[1] = (byte) length;
        // 类型
        header[2] = type;
        // 加密信息
        header[3] = flag;
        // 唯一标识
        header[4] = (byte) identifier;
        // 预留空间
        header[5] = 0;
    }

    public Frame(byte[] header) {
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }

    /**
     * 获取Body的长度
     * @return 当前帧Body总长度[0~MAX_CAPACITY]
     */
    public int getFrameBodyLength() {
        return (((int) header[0]) & 0xFF) << 8 | (((int) header[1]) & 0xFF);
    }

    /**
     * 获取Body的类型
     * @return 类型[0~255]
     */
    public byte getFrameType() {
        return header[2];
    }

    /**
     * 获取Body的Flag
     * @return Flag
     */
    public byte getFrameFlag() {
        return header[3];
    }

    /**
     * 获取Body的唯一标志
     * @return 标志[0~255]
     */
    public short getFrameIdentifier() {
        return (short) (((short) header[4]) & 0xFF);
    }

    /**
     * 进行数据读或写操作
     * @param parameter 数据
     * @return 是否已消费完全， True：则无需再传递数据到Frame或从当前Frame读取数据
     */
    public abstract boolean handle(IOParameter parameter) throws IOException;

    /**
     * 基于当前帧尝试构建下一份待消费的帧
     * @return NULL：没有待消费的帧
     */
    public abstract Frame nextFrame();
}

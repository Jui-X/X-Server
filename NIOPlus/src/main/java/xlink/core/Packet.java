package xlink.core;

import java.io.Closeable;

/**
 * @param: none
 * @description: 公共数据的封装
 *               提供了类型以及基本长度的定义
 * @author: KingJ
 * @create: 2019-06-28 16:50
 **/
public abstract class Packet implements Closeable {
    // 数据类型
    protected byte type;
    // 数据长度
    protected int length;

    public byte type() {
        return type;
    }

    public int length() {
        return length;
    }
}

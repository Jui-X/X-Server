package xlink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @param: none
 * @description: 接收数据的接口类
 * @author: KingJ
 * @create: 2019-06-23 19:49
 **/
public interface Receiver extends Closeable {

    boolean receiveAsync(IOParameter.IOParaEventListener listener) throws IOException;
}

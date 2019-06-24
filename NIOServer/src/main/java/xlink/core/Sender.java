package xlink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @param: none
 * @description: 发送数据的接口类
 * @author: KingJ
 * @create: 2019-06-23 19:49
 **/
public interface Sender extends Closeable {

    boolean sendAsync(IOParameter parameter, IOParameter.IOParaEventListener listener) throws IOException;
}

package audio;

import handler.ConnectorHandler;
import handler.ConnectorHandlerChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-09 21:10
 **/
public class AudioRoom {
    private final String roomCode;
    private volatile ConnectorHandler handler1;
    private volatile ConnectorHandler handler2;

    public AudioRoom() {
        this.roomCode = getRandomString(5);
    }

    /**
     * 生成一个简单的随机字符串
     */
    private String getRandomString(final int length) {
        final String str = "123456789";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public String getRoomCode() {
        return roomCode;
    }

    public ConnectorHandler[] getConnectors() {
        List<ConnectorHandler> handlers = new ArrayList<>();
        if (handler1 != null) {
            handlers.add(handler1);
        }
        if (handler2 != null) {
            handlers.add(handler2);
        }
        return handlers.toArray(new ConnectorHandler[0]);
    }

    /**
     * 获取对方
     */
    public ConnectorHandler getOtherHandler(ConnectorHandler handler) {
        return (handler1 == handler || handler1 == null) ? handler2 : handler1;
    }

    /**
     * 房间是否可聊天，是否两个客户端都具有
     */
    public synchronized boolean isEnable() {
        return handler1 != null && handler2 != null;
    }

    /**
     * 加入房间
     *
     * @return 加入是否成功
     */
    public synchronized boolean enterRoom(ConnectorHandler handler) {
        if (handler1 == null) {
            handler1 = handler;
        } else if (handler2 == null) {
            handler1 = handler;
        } else {
            return false;
        }
        return true;
    }

    public synchronized ConnectorHandler exitRoom(ConnectorHandler handler) {
        if (handler1 == handler) {
            handler1 = null;
        } else if (handler2 == handler) {
            handler2 = null;
        }

        return handler1 == null ? handler2 : handler1;
    }
}

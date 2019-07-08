import box.StringReceivePacket;
import handler.ClientHandler;
import handler.ConnectorHandlerChain;
import handler.ConnectorStringPacketChain;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 10:40
 **/
public class ServerStatistics {
    long receiveSize;
    long sendSize;

    ConnectorStringPacketChain statisticsChain() {
        return new StatisticsConnectorStringPacketChain();
    }

    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            // 接受数据自增
            receiveSize++;
            return false;
        }
    }
}

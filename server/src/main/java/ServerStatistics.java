import box.StringReceivePacket;
import handler.ConnectorHandler;
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
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            // 接受数据自增
            receiveSize++;
            return false;
        }
    }
}

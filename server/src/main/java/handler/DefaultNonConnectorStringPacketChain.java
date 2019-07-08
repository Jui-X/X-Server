package handler;

import box.StringReceivePacket;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 22:31
 **/
public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
        return false;
    }
}

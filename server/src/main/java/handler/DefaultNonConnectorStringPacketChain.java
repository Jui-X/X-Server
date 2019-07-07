package handler;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 22:31
 **/
public class DefaultNonConnectorStringPacketChain extends ConnectorHandlerChain {
    @Override
    protected boolean consume(ClientHandler handler, ConnectorHandlerChain next) {
        return false;
    }
}

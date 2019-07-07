package handler;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 22:32
 **/
public class DefaultPrintConnectorCloseChain extends ConnectorHandlerChain {
    @Override
    protected boolean consume(ClientHandler handler, ConnectorHandlerChain next) {
        System.out.println(handler.getClientInfo() + " Exit! Key " + handler.getKey().toString());
        return false;
    }
}

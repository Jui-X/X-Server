package handler;

import core.Connector;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 22:32
 **/
public class DefaultPrintConnectorCloseChain extends ConnectCloseChain {

    @Override
    protected boolean consume(ClientHandler handler, Connector connector) {
        System.out.println(handler.getClientInfo() + " Exit! Key " + handler.getKey().toString());
        return false;
    }
}

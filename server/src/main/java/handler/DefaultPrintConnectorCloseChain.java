package handler;

import core.Connector;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 22:32
 **/
public class DefaultPrintConnectorCloseChain extends ConnectorCloseChain {

    @Override
    protected boolean consume(ConnectorHandler handler, Connector connector) {
        System.out.println(handler.getClientInfo() + " Exit! Key " + handler.getKey().toString());
        return false;
    }
}

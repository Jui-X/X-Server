package handler;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-07 22:03
 **/
public abstract class ConnectorHandlerChain<Model> {
    private volatile ConnectorHandlerChain<Model> next;

    public ConnectorHandlerChain<Model> appendLast(ConnectorHandlerChain<Model> newChain) {
        if (newChain == this || this.getClass().equals(newChain.getClass())) {
            return this;
        }

        synchronized (this) {
            if (next == null) {
                next = newChain;
                return newChain;
            }
            return next.appendLast(newChain);
        }
    }

    /**
     * 1.移除节点中的某一个节点及其之后的节点
     * 2.移除某节点时，如果其有后续的节点，则将后续节点接到当前节点上，以此实现移除中间节点
     * @param clz 移除节点的class type
     * @return
     */
    public synchronized boolean remove(Class<? extends ConnectorHandlerChain<Model>> clz) {
        if (this.getClass().equals(clz)) {
            return false;
        } else if (next.getClass().equals(clz)) {
            next = next.next;
            return true;
        } else {
            return next.remove(clz);
        }
    }

    /**
     * 优先自己消费，如果自己未消费，则给next消费
     * 若next == null或next未消费，则回调{@link #consumeAgain(ClientHandler, Object)} 尝试再次消费
     * @param handler
     * @param model
     * @return
     */
    synchronized boolean handle(ClientHandler handler, Model model) {
        ConnectorHandlerChain<Model> next = this.next;

        if (consume(handler, next)) {
            return true;
        }

        boolean consumed = next != null && next.handle(handler, model);

        if (consumed) {
            return true;
        }

        return consumeAgain(handler, model);
    }

    protected abstract boolean consume(ClientHandler handler, ConnectorHandlerChain<Model> next);

    protected boolean consumeAgain(ClientHandler handler, Model model) {
        return false;
    }
}

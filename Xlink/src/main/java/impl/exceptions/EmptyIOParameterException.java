package impl.exceptions;

import java.io.IOException;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-11 11:18
 **/
public class EmptyIOParameterException extends IOException {
    public EmptyIOParameterException(String message) {
        super(message);
    }
}

package wsa.exceptions;

import java.net.URISyntaxException;

/**
 * Created by gaamda on 12/11/15.
 *
 * Eccezione specifica per oggetti dominio.
 */
public class DominioException extends URISyntaxException {

    public DominioException(String input, String reason) {
        super(input, reason);
    }
}

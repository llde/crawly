package wsa.exceptions;

/**
 * Created by gaamda on 12/11/15.
 *
 * L'eccezione per i seeds.
 */
public class SeedException extends DominioException {

    public SeedException(String input, String reason) {
        super(input, reason);
    }
}

package wsa.exceptions;

import wsa.web.SiteCrawler;

/**
 * Created by lorenzo on 2/12/16.
 */
public class NotAVisitException extends Exception {

    public NotAVisitException(SiteCrawler sss) {
        super("Non ho trovato una visita valida per : " + sss.getClass().getCanonicalName());
    }
}

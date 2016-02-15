package wsa.exceptions;

import wsa.web.SiteCrawler;

/**
 * Created by lorenzo on 2/12/16.
 */
public class VisitException extends Exception {
    //TODO Investigate I/O Permission denied
    public enum VisitState{NOT_RECOGNIZABLE, CORRUPTED}
    public VisitException(SiteCrawler sss, VisitState state) {
        super( state == VisitState.NOT_RECOGNIZABLE ?  "Non ho trovato una visita valida per : " : "LA visita sembra corrotta per: "
                + sss.getClass().getCanonicalName());
    }
}

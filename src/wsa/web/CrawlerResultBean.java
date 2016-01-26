package wsa.web;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by lorenzo on 26/08/15.
 */
/** Il risultato del tentativo di scaricare una pagina tramite un Crawler
 * JavaBeanVersion */

public class CrawlerResultBean {
    /** L'URI della pagina o null. Se null, significa che la prossima pagina
     * non è ancora pronta. */
    public  URI uri;
    /** true se l'URI è di una pagina i cui link sono seguiti. Se false i campi
     * links e errRawLinks sono null. */
    public  boolean linkPage;
    /** La lista degli URI assoluti dei link della pagina o null */
    public  List<URI> links;
    /** La lista dei link che non è stato possibile trasformare in URI assoluti
     * o null */
    public  List<String> errRawLinks;
    /** Se è null, la pagina è stata scaricata altrimenti non è stato possibile
     * scaricarla e l'eccezione ne dà la causa. Questa è solo la stringa della causa perchè Exception non è java bean complaint */
    public  String exc;

    public CrawlerResultBean(URI u, boolean lp, List<URI> ll, List<String> erl,
                         Exception e) {
        uri = u;
        linkPage = lp;
        links = ll;
        errRawLinks = erl;
        if(e == null) {
            exc = null;
        }
        else{
            exc = e.getMessage();
            if(exc == null) exc = "Download Fallito";   //Fallback se il loader attivo non fornisce un messaggio di errore adeguato
            else if(exc.equalsIgnoreCase(""))  exc = "Download Fallito";   //idem
        }
    }


    public CrawlerResultBean(){
        this(null, false, null, null, null);
    }


    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public boolean isLinkPage() {
        return linkPage;
    }

    public void setLinkPage(boolean linkPage) {
        this.linkPage = linkPage;
    }

    public List<URI> getLinks() {
        return links;
    }

    public void setLinks(List<URI> links) {
        this.links = links;
    }

    public String getExc() {
        return exc;
    }

    public void setExc(String exc) {
        this.exc = exc;
    }

    public List<String> getErrRawLinks() {
        return errRawLinks;
    }

    public void setErrRawLinks(List<String> errRawLinks) {
        this.errRawLinks = errRawLinks;
    }


    public CrawlerResult getCrawlerResult(){
        Exception e = null;
        if(exc != null){
            e = new Exception(exc);
        }
        return new CrawlerResult(uri,linkPage,links,errRawLinks, e);
    }


    public static CrawlerResultBean getCRBean(CrawlerResult cr){
        return new CrawlerResultBean(cr.uri,cr.linkPage, cr.links, cr.errRawLinks,cr.exc);
    }
}

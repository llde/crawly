package wsa.web;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import wsa.web.LoadResult;
import wsa.web.Loader;
import wsa.web.LoaderFactory;
import wsa.web.html.*;
import wsa.web.html.Parsed;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Takeru on 24/06/15.
 */
public class HelperLoader implements Loader,LoaderFactory {
    private   WebEngine webweb;
    private   Document doc = null;
    private   boolean errore = false;
    private   Exception ex;
    Map<String,String> mappa= new HashMap<String, String>();
    private  volatile HelperParsed hp = new HelperParsed();
    Set<Parsed.Node> setnodi = new HashSet<Parsed.Node>();
    HelperParsed p;
    public  boolean caricamentofinito = false;
    Boolean finito;

    public HelperLoader(){
        Platform.runLater(() -> { doc = null; });
    }

    /**
     * Ritorna il risultato del tentativo di scaricare la pagina specificata. È
     * bloccante, finchè l'operazione non è conclusa non ritorna.
     * Operazioni:
     * -carica una pagina vuota per resettare la web engine
     * -gestite lo stato del WORKER
     * -
     * @param url l'URL di una pagina web
     * @return il risultato del tentativo di scaricare la pagina
     */
    @Override
    public LoadResult load(URL url) {
        finito=false;
        Platform.setImplicitExit(true);
        Platform.runLater(() -> {
            webweb = new WebEngine();
            webweb.getHistory().setMaxSize(0);
            webweb.setJavaScriptEnabled(false);
            webweb.setUserAgent("Mozilla/5.0");
            doc = null;
            webweb.load("");
            webweb.getLoadWorker().stateProperty().addListener((o, os, ns) -> {
                if (ns == Worker.State.SUCCEEDED) {
                    setnodi.clear();
                    Map<String, String> mappa = new HashMap<String, String>();
                    NodeList list;
                    if(webweb.getDocument()!=null) {
                        list = webweb.getDocument().getChildNodes();
                        nodiricorsivi(list);
                    }
                    p = new HelperParsed();
                    p.setta = setnodi;
                    list = null;
                    caricamentofinito = true;
                    webweb=null;
                    doc=null;
                    mappa=null;
                    finito = true;
                }
                if (ns == Worker.State.FAILED) {
                    ex = new Exception(webweb.getLoadWorker().getException().getMessage(), webweb.getLoadWorker().getException().getCause());
                    webweb = null;
                    setnodi.clear();
                    errore = true;

                }
                if (ns == Worker.State.CANCELLED) {
                    ex=new Exception("Caricamento annullato");
                    webweb = null;
                    setnodi.clear();
                    finito = true;
                }
            });

            try {
                webweb.load(url.toString());
            } catch (Exception ew) {}
        });

        while((doc==null)){

            if(finito) break;
            try{
                Thread.sleep(10);
            }catch (Exception e){
                break;
            }
            if(errore) break;
        }
        return new LoadResult(url,p,ex);
    }

    /**
     * Metodo ricorsivo serve per scorrere tutti i nodi interni ad una NodeList e creare una mappa e in un secondo momento aggiungere in un insieme
     * @param nl NodeList da scorrere
     */
    void nodiricorsivi(NodeList nl){
        for (int i = 0; i < nl.getLength(); i++) {
            mappa = new HashMap<>();
            NamedNodeMap nnm = nl.item(i).getAttributes();
            if (nnm!=null) {
                for (int pop = 0; pop < nnm.getLength(); pop++){
                    mappa.put(nnm.item(pop).getNodeName(),nnm.item(pop).getNodeValue());
                }
            }
            setnodi.add(hp.creanodo(nl.item(i).getNodeName(),mappa,nl.item(i).getNodeValue()));
            NodeList list2 = nl.item(i).getChildNodes();
            nodiricorsivi(list2);
        }
    }

    /**
     * Ritorna null se l'URL è scaricabile senza errori, altrimenti ritorna
     * un'eccezione che riporta l'errore.
     *
     * @param url un URL
     * @return null se l'URL è scaricabile senza errori, altrimenti
     * l'eccezione
     */
    @Override
    public Exception check(URL url) {
        try {
            URL urlO = url;
            URLConnection urlC = urlO.openConnection();
            urlC.setRequestProperty("User-Agent", "Mozilla/5.0");
            urlC.setRequestProperty("Accept", "text/html;q=1.0,*;q=0");
            urlC.setRequestProperty("Accept-Encoding", "identity;q=1.0,*;q=0");
            urlC.setConnectTimeout(5000);
            urlC.setReadTimeout(10000);
            urlC.connect();
        }catch (Exception e){
            return e;
        }
        return null;
    }

    /**
     * Ritorna una nuova istanza di un Loader
     *
     * @return una nuova istanza di un Loader
     */
    @Override
    public Loader newInstance() {
        return this;
    }
}

package wsa.web;

/**
 * Created by lorenzo on 17/11/15.
 */

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import wsa.web.html.Parsed;
import wsa.web.html.Parsing;

import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementazione sperimentale di un loader che usa notify() e wait() al
 */
public class GondorLoader implements Loader
{
    // Grabber per notify
    private final Object mybase = new Object();

    private final AtomicReference<Worker.State> downloadRes = new AtomicReference<>(null);
    private final AtomicReference<Parsed> parser = new AtomicReference<>(null);
    private final AtomicReference<WebEngine> we = new AtomicReference<>(null);
    private volatile String err = null;
    GondorLoader() {
        Platform.runLater(() -> {
            we.set(new WebEngine(""));
            we.get().setJavaScriptEnabled(false); //Settare questo a True provoca problemi gravi nel ritorno del Document e nel suo parsing.
            // Alternativamente lo scaricamento può risultare cancellato.
            //Questo su sistema arch linux
            we.get().getLoadWorker().stateProperty().addListener((o, ov, nv) -> {
                if (nv == Worker.State.SUCCEEDED || nv == Worker.State.FAILED || nv == Worker.State.CANCELLED) {
                    if (!we.get().getLocation().equalsIgnoreCase("about:blank") && !we.get().getLocation().equalsIgnoreCase("")) {
                        //   System.err.println(we.get().getLocation());   //Uncomment for fhurther debug
                        downloadRes.set(nv);
                        if (nv == Worker.State.SUCCEEDED) {
                            if (we.get().getDocument() != null) {
                                parser.set(WebFactory.getParsed(we.get().getDocument()));
                                synchronized (mybase)  {mybase.notify();}
                            } else {
                                downloadRes.set(Worker.State.FAILED);
                                err = "Il Document è risultato nullo nonostante la webengine abbia mostrato un successo dello scaricamento.";
                                synchronized (mybase)  {mybase.notify();}
                            }
                        } else if (nv == Worker.State.FAILED || nv == Worker.State.CANCELLED) {
                            if(downloadRes.get() == Worker.State.FAILED) err = "Scaricamento fallito";
                            else if(downloadRes.get() == Worker.State.CANCELLED) err = "Scaricamento cancellato";
                            //Scaricamento cancellato può accadere in caso di siti che montano una DDOS protection, anche con setJavaScriptEnabled(false)
                            synchronized (mybase)  {mybase.notify();}
                        }
                    }
                }
            });
        });
    }


    @Override
    public synchronized LoadResult load(URL url) {
        err = null;
        Exception t;
        downloadRes.set(null);
        parser.set(null);
        t = check(url);
        if(t != null) return new LoadResult(url, null, t);
        Platform.runLater(() -> {
            try {
                we.get().load(url.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        synchronized (mybase)  {
            try {
                mybase.wait();
            } catch (InterruptedException ignored) {}
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.out.println("Ehi cattivo! Mi hai chiuso... Piango :'(");
        }
        Platform.runLater(() -> we.get().load(null));
        if (downloadRes.get() == Worker.State.CANCELLED || downloadRes.get() == Worker.State.FAILED) {                //Casi contemplati
            return new LoadResult(url, null, new Exception(err));
        }
        else{
            return new LoadResult(url, parser.get(), null);
        }                                          //Caso SUCCEDED
    }

    @Override
    public Exception check(URL url) {               //Ritorna eventuali errori nel Download della pagina
        try {
            URLConnection connessione = url.openConnection();                            //Parte presa dalla lezione
            connessione.setRequestProperty("User-Agent", "Mozilla/5.0");
            connessione.setRequestProperty("Accept", "text/html;q=1.0,*;q=0");
            connessione.setRequestProperty("Accept-Encoding", "identity;q=1.0,*;q=0");
            connessione.setConnectTimeout(5000);
            connessione.setReadTimeout(10000);
            connessione.connect();}
        catch(Exception e){
            return e;
        }
        return null;
    }
}
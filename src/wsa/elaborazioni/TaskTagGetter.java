package wsa.elaborazioni;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import wsa.web.html.Parsed;
import wsa.web.html.Parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaamda on 29/11/15.
 *
 * Un task specializzato nella raccolta di tags da una pagina web uri.
 * Utilizza parsing fornito da {@link Parsing} per ottenere i dati e ritorna, se possibile,
 * una lista di {@link wsa.web.html.Parsed.Node} contenenti i tags ricercati.
 * Nel caso di fallimento il risultato viene portato a lista vuota.
 */
public class TaskTagGetter implements Task<List<Parsed.Node>> {

    private ObjectProperty<Worker.State> stato = new SimpleObjectProperty<>(Worker.State.SCHEDULED);
    private WebEngine we;
    private String tag;
    private String url;
    private Integer nodesNumber = 0;
    private List<Parsed.Node> res;

    /**
     * Crea un TaskTagGetter con i parametri forniti. Questo Task scaricherà una pagina usando solo {@link WebEngine}
     * di javaFx. Da essa elaborerà le informazioni.
     * @param url L'url da scaricare.
     * @param tag Il tag da cercare.
     * @throws IllegalArgumentException se i valori sono empty.
     */
    public TaskTagGetter(String url, String tag) throws IllegalArgumentException{
        if (url.isEmpty() || tag.isEmpty()) throw new IllegalArgumentException();
        this.tag = tag;
        this.url = url;
    }

    /**
     * Ottiene il numero di nodi dell'albero di parsing, se lo stato è SUCCEDED.
     * Se ci sono stati errori, o la pagina non è ancora stata scaricata, il numero è 0.
     * @return Il numero dei nodi dell'albero di parsing della pagina appena scaricata.
     */
    public Integer getNodes(){
        return nodesNumber;
    }

    @Override
    public void esegui(Executor e) {
        Platform.runLater(() -> {
            we = new WebEngine("");
            we.setJavaScriptEnabled(false);
            we.getLoadWorker().stateProperty().addListener((o, ov, nv) -> {
                if (nv == Worker.State.SUCCEEDED
                        && !we.getLocation().equalsIgnoreCase("about:blank")
                        && !we.getLocation().equalsIgnoreCase("")
                        && we.getDocument() != null){
                    Parsing ps = new Parsing(we.getDocument()); /* verrà resa null per aiutare il GC */

                    res = ps.getByTag(tag);
                    ps.visit(node -> nodesNumber += 1); /* Esegue una visita per contare i nodi */
                    ps = null;

                    stato.setValue(Worker.State.SUCCEEDED);

                }else if (nv == Worker.State.FAILED || nv == Worker.State.CANCELLED){
                    res = new ArrayList<>();
                    stato.setValue(Worker.State.FAILED);
                }
            });
            we.load(url); /* Esegue il caricamento della pagina */
        });
    }

    @Override
    public List<Parsed.Node> getData() {
        return res;
    }

    @Override
    public Worker.State getTaskState() {
        return stato.get();
    }

    @Override
    public ReadOnlyObjectProperty<Worker.State> getWorkerState() {
        return stato;
    }
}

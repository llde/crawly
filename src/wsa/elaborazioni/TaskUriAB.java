package wsa.elaborazioni;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import wsa.gui.TabFrame;
import wsa.web.SiteCrawler;

import java.net.URI;

/**
 * Created by gaamda on 13/12/15.
 *
 * Un task asincrono per la finestra di azioni su più visite.
 */
public class TaskUriAB implements Task {

    private ObjectProperty<Worker.State> stato = new SimpleObjectProperty<>(Worker.State.SCHEDULED);
    private ObservableList<URI> ret = FXCollections.observableArrayList();
    private TabFrame A = null;
    private TabFrame B = null;

    /**
     * Crea un task per la computazione degli uri da un tabFrame A ad un tabFrame B.
     * Sia p una pagina di B. Allora p è soluzione se e solo se p appartiene al dominio di A.
     * Per coerenza, se p viene linkata da una pagina di A, allora l'algoritmo mostrerà la pagina
     * di A che linka p.
     * @param A Il tabframe di partenza.
     * @param B Il tabframe di arrivo.
     */
    public TaskUriAB(TabFrame A, TabFrame B){
        this.A = A;
        this.B = B;
    }

    @Override
    public void esegui(Executor e) {
        if (A == null || B == null){
            this.stato.setValue(Worker.State.FAILED);
            return;
        }
        else{
            A.getData().forEach(page -> {   //per ogni seed dell'esplorazione A
                try {
                    if (SiteCrawler.checkSeed(B.getDom().getURI(), page.getURI())) {     //se il seed è interno al dominio dell'esplorazione B
                        if (page.getPageLink()) {           //se il seed è interno anche al dominio dell'esplorazione A (la propria) aggiungilo alla lista, i links a cui punta saranno tra quelli scorsi da questo forEach
                            ret.add(page.getURI());
                        }else{                              //se il seed NON è interno anche al dominio dell'esplorazione A, allora vuol dire che appartine solo al dominio B,
                            ret.addAll(page.getPtr());      //e che quindi tutte le pagine che puntano ad esso (e per puntare devono appartenere ad A) vanno anche in B. Lo aggiunge alla lista.
                        }
                    }
                }catch (Exception ignored){}    // Volutamente ignorata.
            });
        }
        this.stato.setValue(Worker.State.SUCCEEDED);
    }

    @Override
    public ObservableList<URI> getData() {
        return ret;
    }

    @Override
    public Worker.State getTaskState() {
        return this.stato.getValue();
    }

    @Override
    public ReadOnlyObjectProperty<Worker.State> getWorkerState() {
        return this.stato;
    }

}

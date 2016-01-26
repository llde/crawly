package wsa.API;

import javafx.beans.property.SimpleObjectProperty;

import java.util.concurrent.Callable;

/**
 * Classe logica che wrappa un valore T ed uno G, utile per mappe dai valori misti.
 * L'elemento creato NON E', per decisione, modificabile. Se in errore, istanziare un
 * secondo oggetto Wrap.
 * Non sono ammessi runnables o callables.
 * @param <T> La chiave T.
 * @param <G> Il valore G.
 */
public class Wrap<T, G> {
    public final SimpleObjectProperty<T> key = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<G> val = new SimpleObjectProperty<>();

    public Wrap(T key, G val){
        if (!(key instanceof Runnable || key instanceof Callable)) {
            this.key.setValue(key);
        }else{
            this.key.setValue(null);
        }
        if (!(val instanceof Runnable || val instanceof Callable)) {
            this.val.setValue(val);
        }else{
            this.val.setValue(null);
        }
    }
}
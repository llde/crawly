package wsa.web;

import java.io.Serializable;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Map;

/**
 * Created by lorenzo on 1/15/16.
 * Una particolare struttura dati similare a Map.SimpleMapEntry
 * Ma che consente il salvataggio con XMLEncoder
 */
public class SaveEntry<T,K> implements Serializable {


    private T key;
    private K value;

    /**
     *
     *Costruttore normale prende una chiave e un valore
     * @param ur la chiave di tipo T
     * @param resb il valore di tipo K
     */
    public SaveEntry(T ur, K resb) {
        key = ur;
        value = resb;
    }

    /**
     * Costruttore che permette la creazione di una SaveEntry a partire da
     * un' oggetto Map.Entry<T,K>
     */
    public SaveEntry(Map.Entry<T,K> entry) {
        key = entry.getKey();
        value = entry.getValue();
    }


    //Metodi richiesti dalla convenzione JavaBean

    /**
     * Costruttore senza argomenti, per specifica Java Bean
     */
    public SaveEntry(){
        key = null;
        value = null;
    }


    /**
     * Permette di ottenere l' oggetto chiave della entry.
     * @return key
     */
    public T getKey() {
        return key;
    }


    /**
     * Permette di settare l'oggetto chiave della entry
     * @param key l'oggetto da mettere come chiave
    */
    public void setKey(T key) {
        this.key = key;
    }

    /**
     * Permette di ottenere l' oggetto valore della entry.
     * @return value
     */
    public K getValue() {
        return value;
    }

    /**
     * Permette di settare l'oggetto valore della entry
     * @param key l'oggetto da mettere come valore
     */
    public void setValue(K value) {
        this.value = value;
    }

}

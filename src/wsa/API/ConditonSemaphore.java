package wsa.API;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Un Semaphore che usa una condizione di rilascio.
 * Questa classe permette di gestire una pausa condizionata dei thread, similmente ad una barrier, usando
 * internamente un semaforo {@link java.util.concurrent.Semaphore} e garantendo - quindi - passive waiting.
 */
public class ConditonSemaphore<T> {
    private volatile Semaphore semaforo = null;
    private Predicate<T> condition = null;
    private T conditionedobject = null;
    private int numeromaxthread = 0;
    private final AtomicBoolean disabled = new AtomicBoolean(false);
    private boolean negated;

    /**
     * Crea un ConditionSemaphore con i seguenti parametri.
     * @param pred il predicato che viene testato per il blocco dei thread chimanti
     * @param conditionedObject l'oggetto sul quale il predicato va eseguito
     * @param numeromaxthreads il numero massimo di thread che sono collegati la semaforo
     * @param negate Se true il semaforo blocca i thread quando il predicato è false altrimenti quando è true
     */
    public ConditonSemaphore(Predicate<T> pred , T conditionedObject, int numeromaxthreads, boolean negate){
        semaforo = new Semaphore(0);
        condition = pred;
        conditionedobject = conditionedObject;
        numeromaxthread = numeromaxthreads;
        negated = negate;
    }

    /**
     * Controlla se il predicato è vero(con negated false) o falso (con negated true), in qual caso blocca l'esecuzione del thread
     * @throws InterruptedException se il thread in attesa viene interrotto.
     */
    public void acquire() throws InterruptedException {
        if(disabled.get()) return;
        if(negated) {
            if (!condition.test(conditionedobject)) {
                System.out.println("Locked  " + conditionedobject.toString());
                semaforo.acquire();
            }
        }
        else {
            if (condition.test(conditionedobject)) {
                System.out.println("Locked  " + conditionedobject.toString());
                semaforo.acquire();
            }
        }
    }

    /**
     * Controlla se il predicato è falso(con negated false) o vero (con negated true), nel caso rilascia tutti i thread
     * che sono stati bloccati da acquire.
     */
    public void release(){
        if(disabled.get()) return;
        if(negated) {
            if (condition.test(conditionedobject)) {
                semaforo.release(numeromaxthread + 1);
                semaforo = new Semaphore(0);
            }
        }
        else {
            if (!condition.test(conditionedobject)) {
                semaforo.release(numeromaxthread + 1);
                semaforo = new Semaphore(0);
            }
        }
    }

    /**
     * Rilascia forzatamente tutti i threads di questo ConditionSemaphore,
     * ripristinando il Semaphore interno a 0.
     */
    private void releaseForced(){
        semaforo.release(numeromaxthread + 1);
        semaforo = new Semaphore(0);
    }

    /**
     * Disabilita questo ConditionSemaphore, rilasciando
     * tutti i thread in attesa.
     */
    public void Disable(){
        disabled.set(true);
        this.releaseForced();
    }

    /**
     * Abilita questo ConditionSemaphore, in modo che possa
     * (ri)cominciare a mettere in pausa i thread.
     */
    public void Enable(){
        disabled.set(false);
    }
}

package wsa.web;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import wsa.session.DataGate;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Created by Lorenzo on 23/08/2015.
 * Un' implementazione dell'interfaccia SiteCrawler
 */

public class AveSithisSiteCralwer implements SiteCrawler {
    private enum SiteCrawlerMode{EXPLORATION_ONLY, EXPLORATION_SAVE, LOAD_EXPLORATION_SAVE}
    private enum SiteCrawlerState{INIT,RUNNING, SUSPENDED, CANCELED , TERMINATED_PROGRESSION_ACTIVE, TERMINATED}
    private final AtomicReference<Crawler> crawl = new AtomicReference<>(null);
    private final AtomicReference<SiteCrawlerState> state = new AtomicReference<>(null);
    private SiteCrawlerMode mode = null;
    private URI dominio = null;
    private Path archiviazione = null;
    private final int tempodisalavataggio = 10; //TODO settings
    private List<URI> toLoadTemp = null;
    private List<URI> LoadedTemp = null;
    private List<URI> errorsTemp = null;
    private List<CrawlerResult> progressionTemp = null;
    private Thread site = null;
    private Thread exceptionSave = null;
    private DataGate dataStruct = null;
    //Per creare thread di salvataggio, quando il thread principale (site) non dovrebbe essere attivo
    //Viene usato dal metodo suspend
    private Runnable exceptionalsave = () ->{
        if(!site.isAlive())  //Se a questo punto il thread di salvataggio è ancora in vita sta probabilmente salvando o ha finito da poco
            SaveVisit();    //Inutile risalvare
    };

    private final Runnable run =  () -> {  //Salvataggio asincrono.
        while(true){                         //Fa il lavoro di un timer, salva ogni tot sencondi.
            if(Thread.interrupted()) break;
            try {
                Thread.sleep(tempodisalavataggio*1000);
            } catch (InterruptedException e) {
                break;
            }
            this.SaveVisit();
        }
    };

    private ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // configure kryo instance, customize settings
            return kryo;
        };
    };

    AveSithisSiteCralwer(URI dom, Path dir) throws IOException {
        if (dom == null && dir == null) throw new IllegalArgumentException("Non posso creare un SiteCrawler con questi paramentri");
        if(dom != null) if(!SiteCrawler.checkDomain(dom))  throw new IllegalArgumentException("Devi passarmi un dominio valido!!!");
        if (dom == null) {
            Path file = Paths.get(dir.toString(), "visita.crawly");
            System.out.println(file);
            if(!dir.toFile().isDirectory() || !file.toFile().exists()) throw new IllegalArgumentException("Non è un archivio di visita di questo SiteCralwer");
            System.out.println("Visita in ripresa");
            mode = SiteCrawlerMode.LOAD_EXPLORATION_SAVE;
            state.set(SiteCrawlerState.INIT);
            archiviazione = dir;
            this.LoadVisit();
            crawl.set(WebFactory.getCrawler(LoadedTemp, toLoadTemp, errorsTemp, (uri -> SiteCrawler.checkSeed(dominio, uri))));
        }  //Caso ripresa esplorazione.

        else if(dir == null)  {
            mode = SiteCrawlerMode.EXPLORATION_ONLY;
            state.set(SiteCrawlerState.INIT);
            dominio = dom;
            crawl.set(WebFactory.getCrawler(new HashSet<>(), new HashSet<>(), new HashSet<>(), (ur) -> SiteCrawler.checkSeed(dominio, ur)));
        }  //Caso solo visita.

        else {
            mode = SiteCrawlerMode.EXPLORATION_SAVE;
            state.set(SiteCrawlerState.INIT);
            crawl.set(WebFactory.getCrawler(new HashSet<>(), new HashSet<>(), new HashSet<>(), (ur) -> SiteCrawler.checkSeed(dominio, ur)));
            dominio = dom;
            archiviazione = dir;   //Controllare la presenza di un archivio già presente.
        }  //Caso visita con salvataggio
        this.dataStruct = this.crawl.get().getData(); /* Getting datagate */
        if(progressionTemp != null) progressionTemp.forEach((xx) -> this.dataStruct.add(xx)); //Se sto recuperando la visita popolo il DataGate
    }

    /**
     * Aggiunge un seed URI. Se però è presente tra quelli già scaricati,
     * quelli ancora da scaricare o quelli che sono andati in errore,
     * l'aggiunta non ha nessun effetto. Se invece è un nuovo URI, è aggiunto
     * all'insieme di quelli da scaricare.
     *
     * @param uri un URI
     * @throws IllegalArgumentException se uri non appartiene al dominio di
     *                                  questo SuteCrawlerrawler
     * @throws IllegalStateException    se il SiteCrawler è cancellato
     */
    @Override
    public void addSeed(URI uri) {
        throwIfCancelled();
        if(getToLoad().contains(uri) || getLoaded().contains(uri) || getErrors().contains(uri)) return;
        crawl.get().add(uri);
        if(state.get() == SiteCrawlerState.TERMINATED || state.get() == SiteCrawlerState.TERMINATED_PROGRESSION_ACTIVE){
            this.start();
        }
    }

    @Override
    public DataGate getData() {
        return this.dataStruct;
    }

    @Override
    public void resubmit(URI uri) {
        throwIfCancelled();
        if(getToLoad().contains(uri)) return;
        crawl.get().resubmit(uri);
        if(state.get() == SiteCrawlerState.TERMINATED || state.get() == SiteCrawlerState.TERMINATED_PROGRESSION_ACTIVE){
            this.start();
        }
    }

    /**
     * Inizia l'esecuzione del SiteCrawler se non è già in esecuzione e ci sono
     * URI da scaricare, altrimenti l'invocazione è ignorata. Quando è in
     * esecuzione il metodo isRunning ritorna true.
     *
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public void start() {
        throwIfCancelled();
        if(isRunning()) return;
        if(state.get() == SiteCrawlerState.INIT || state.get() == SiteCrawlerState.SUSPENDED || state.get() == SiteCrawlerState.TERMINATED || state.get() == SiteCrawlerState.TERMINATED_PROGRESSION_ACTIVE) {
        }
        state.set(SiteCrawlerState.RUNNING);
        if(mode == SiteCrawlerMode.EXPLORATION_ONLY){
            crawl.get().start();
        }
        else if(mode == SiteCrawlerMode.EXPLORATION_SAVE){
            site = new Thread(run);
            site.setName("SiteCrawler Thread- Save Subroutine");
            site.start();
            crawl.get().start();
        }
        else  if(mode == SiteCrawlerMode.LOAD_EXPLORATION_SAVE){
            site = new Thread(run);
            site.setName("SiteCrawler Thread- Save Subroutine");
            site.start();
            crawl.get().start();
        }

    }

    /**
     * Sospende l'esecuzione del SiteCrawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività dovrebbe essere ridotta al minimo possibile
     * (eventuali thread dovrebbero essere terminati). Se è stata specificata
     * una directory per l'archiviazione, lo stato del crawling è archiviato.
     *
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public void suspend() {
        throwIfCancelled();
        if(state.get() != SiteCrawlerState.RUNNING  || state.get() != SiteCrawlerState.TERMINATED_PROGRESSION_ACTIVE) return;
        state.set(SiteCrawlerState.SUSPENDED);
        crawl.get().suspend();
        if(mode != SiteCrawlerMode.EXPLORATION_ONLY ) site.interrupt();
        System.out.println("I'm suspended");
        if(mode != SiteCrawlerMode.EXPLORATION_ONLY) {
            exceptionSave = new Thread(exceptionalsave);
            exceptionSave.start();
        }
        System.out.println(state.get());
    }

    /**
     * Cancella il SiteCrawler per sempre. Dopo questa invocazione il
     * SiteCrawler non può più essere usato. Tutte le risorse sono
     * rilasciate.
     */
    @Override
    public void cancel() {
        if(mode != SiteCrawlerMode.EXPLORATION_ONLY) site.interrupt();
        crawl.get().cancel();
        state.set(SiteCrawlerState.CANCELED);
        toLoadTemp = null;
        LoadedTemp = null;
        errorsTemp = null;
    }


    /**
     * Ritorna il risultato del tentativo di scaricare la pagina che
     * corrisponde all'URI dato.
     *
     * @param uri un URI
     * @return il risultato del tentativo di scaricare la pagina
     * @throws IllegalArgumentException se uri non è nell'insieme degli URI
     *                                  scaricati ne nell'insieme degli URI che hanno prodotto errori.
     * @throws IllegalStateException    se il SiteCrawler è cancellato
     */
    @Override
    public CrawlerResult get(URI uri) {
        throwIfCancelled();
        if(!getErrors().contains(uri) && !getLoaded().contains(uri)) throw new IllegalArgumentException("Non ho scaricato questo uri");
      //  if(progression.containsKey(uri)) return progression.get(uri).getCrawlerResult();
       /* else */ throw new IllegalArgumentException("Ho scaricato l'URI ma ancora non appartiene all' Ave Sithis Site Crawler.");
    }

    /**
     * Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     *
     * @return l'insieme di tutti gli URI scaricati (mai null)
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Set<URI> getLoaded() {
        throwIfCancelled();
        return crawl.get().getLoaded();
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che devono essere
     * ancora scaricati. Quando l'esecuzione del crawler termina normalmente
     * l'insieme è vuoto.
     *
     * @return l'insieme degli URI ancora da scaricare (mai null)
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Set<URI> getToLoad() {
        throwIfCancelled();
        return crawl.get().getToLoad();
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     *
     * @return l'insieme degli URI che hanno prodotto errori (mai null)
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Set<URI> getErrors() {
        throwIfCancelled();
        return crawl.get().getErrors();
    }

    /**
     * Ritorna true se il SiteCrawler è in esecuzione.
     *
     * @return true se il SiteCrawler è in esecuzione
     */
    @Override
    public boolean isRunning() {
        return state.get() == SiteCrawlerState.RUNNING || state.get() == SiteCrawlerState.TERMINATED_PROGRESSION_ACTIVE;
    }

    @Override
    public URI getDomain() {
        return dominio;
    }

    /**
     * Ritorna true se il SiteCrawler è stato cancellato. In tal caso non può
     * più essere usato.
     *
     * @return true se il SiteCrawler è stato cancellato
     */
    @Override
    public boolean isCancelled() {
        return state.get() == SiteCrawlerState.CANCELED;
    }

    @Override
    public boolean isSuspended() {
        return state.get() == SiteCrawlerState.SUSPENDED;
    }

    @Override
    public boolean isTerminated() {
        return state.get() == SiteCrawlerState.TERMINATED;
    }


    /*
     * Salva la visita nella directory specificata
     */
    private synchronized void SaveVisit(){
        try {
            //TODO need to be totally redone.
            Output enc = new Output(new FileOutputStream(archiviazione.toString() + "/visita.crawly"));
            Kryo kry = kryo.get();
            kry.writeObject(enc, dominio);
            kry.writeObject(enc, getToLoad());
            kry.writeObject(enc, getLoaded());
            kry.writeObject(enc, getErrors());
            List<CrawlerResult> cress = new ArrayList<>();
            this.dataStruct.getDataList().forEach(page -> cress.add(page.toCralwerResult()));
            kry.writeObject(enc, cress);
            enc.close();
            System.out.println("Visita salvata");
        } catch (FileNotFoundException ignored) {System.err.println("Saving visit: something went wrong, with file writing");}
    }


    /*
     * Permette il recupero della visita
     */
    @SuppressWarnings("unchecked")
    private void LoadVisit(){
        try(Input dec = new Input(new FileInputStream(archiviazione.toString() + "/visita.crawly"))){
            Kryo kry = kryo.get();
            dominio = kry.readObject(dec, URI.class);
            toLoadTemp = Arrays.asList(kry.readObject(dec, URI[].class));
            LoadedTemp = Arrays.asList(kry.readObject(dec, URI[].class));
            errorsTemp = Arrays.asList(kry.readObject(dec, URI[].class));
            progressionTemp  = new ArrayList<>();
            Arrays.stream(kry.readObject(dec, CrawlerResult[].class)).forEach((cres) -> progressionTemp.add(cres));

        }
        catch (FileNotFoundException e) {
            System.err.println("Loading visit: something went wrong, with file reading");
        }
        //TODO Rerise!
        catch (Exception e){ System.err.println("non ho potuto ricaricare la visita. Forse l'archivio è corrotto?");}
    }

    /**
     * Controlla se il SiteCralwer è cancellato, in caso lancia eccezione
     * @throws IllegalArgumentException
     */
    private void throwIfCancelled(){
        if(isCancelled()) throw new IllegalStateException("Questo AveSithisSiteCrawler è cancellato e non può più essere usato");
    }
}

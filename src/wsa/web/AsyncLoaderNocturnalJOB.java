package wsa.web;

import java.net.URL;
import java.util.concurrent.*;

/**
 * Created by lorenzo on 17/11/15.
 */
public class AsyncLoaderNocturnalJOB implements AsyncLoader {

    private ThreadFactory fact = (r)->{
        Thread t = new Thread(r);
        t.setName("AsyncLoader Nocturnal JOB:  Job ");
        t.setDaemon(true);
        t.setPriority(2);
        return t;
    };

    private boolean shutdown = false;
    private final LinkedBlockingQueue<Loader> codalod = new LinkedBlockingQueue<>(50);
    private ExecutorService exs = Executors.newFixedThreadPool(50,fact);
    private ExecutorService boia = Executors.newFixedThreadPool(10 ,fact);

    AsyncLoaderNocturnalJOB() {
        for (int i = 0; i < 50; i++) {
            codalod.offer(WebFactory.getLoader());
        }
    }

    /**
     * Sottomette il downloading della pagina dello specificato URL e ritorna
     * un Future per ottenere il risultato in modo asincrono.
     *
     * @param url un URL di una pagina web
     * @return Future per ottenere il risultato in modo asincrono
     * @throws IllegalStateException se il loader è chiuso
     */
    @Override
    public Future<LoadResult> submitLoad(URL url) {
        if (isShutdown()) throw new IllegalStateException();
        Callable<LoadResult> async = () -> {
            Loader l = codalod.take();
            System.out.println( "Loader taken: " + codalod.size() + " "  + l.getClass());
            LoadResult res = l.load(url);
            codalod.offer(l);
            return res;
        };
        return exs.submit(async);
    }

    /**
     * Sottomette la richiesta di validazione della pagina dello specificato URL e ritorna
     * un Future per ottenere il risultato in modo asincrono.
     *
     * @param url un URL di una pagina web
     * @return Future per ottenere il risultato in modo asincrono
     * @throws IllegalStateException se il loader è chiuso
     */
    @Override
    public Future<LoadResult> submitCheck(URL url) {
        if(isShutdown()) throw new IllegalArgumentException();
        Callable<LoadResult> async = () ->{
            Loader l = WebFactory.getLoader();
            return l.check(url);
        };
        return boia.submit(async);
    }

    /**
     * Chiude il loader e rilascia tutte le risorse. Dopo di ciò non può più essere usato.*/
    @Override
    public void shutdown() {
        if (!isShutdown()) {
            exs.shutdownNow();
            boia.shutdown();
            shutdown = true;
            exs = null;
        }
    }

    @Override
    public boolean isShutdown(){
        return shutdown;
    }

}
package wsa.session;

import wsa.exceptions.DominioException;
import wsa.web.SiteCrawler;

import java.net.MalformedURLException;

/**
 * Created by gaamda on 03/11/2015.
 *
 * Estende Dominio, fornisce l'URL se la conversione è andata a buon fine.
 */
public class Seed extends Dominio {

    private boolean checked = false;

    /**
     * Crea un nuovo seed con l'uri associato.
     * @param uri L'uri da usare come dominio.
     */
    public Seed(String uri) throws DominioException, MalformedURLException {
        super(uri);
        super.setURL(super.getURI().toURL());
    }

    /**
     * Setta il parametro di checking al valore b.
     * Questo parametro ha senso solo per le routine che vogliono usarlo, ed ha senso solo nel momento
     * della creazione del Seed. È tuttavia mantenuto pubblico per un possibile futuro uso, nonostante
     * sia garantito il suo inutilizzo nel motore di download settato di default.
     * Considerare che, nel modulo GUI, l'unica istanza che tratta questo campo come utile è la finestra
     * di validazione della visita. Essa però crea il Seed, perciò nessun metodo/classe può intromettersi
     * in quella gestione, il malfunzionamento perciò è sventato.
     * @see wsa.gui.VisitFrame
     * @see GestoreDownload
     * @param b il valore sa settare.
     */
    public void setChecked(boolean b){
        this.checked = b;
    }

    /**
     * Ottiene l'attuale parametro di checking, impostato - molto probabilmente - dalla finestra di validazione
     * della visita.
     * @return Il booleano di checking.
     */
    public boolean getChecked(){
        return this.checked;
    }

    public boolean validateSeed(Dominio d) {
        return d != null && SiteCrawler.checkSeed(d.getURI(), this.getURI());
    }
}

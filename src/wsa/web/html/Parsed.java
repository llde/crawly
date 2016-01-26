package wsa.web.html;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Rappresenta l'albero di analisi sintattica (parsing) di una pagina web */
public interface Parsed {
    /** Un nodo dell'albero di parsing della pagina */
    class Node {
        /** Se è un elemento, è il nome del tag, altrimenti è null */
        public final String tag;
        /** Gli attributi dell'elemento o null */
        public final Map<String,String> attr;
        /** Se è un elemento è null, altrimenti è testo */
        public final String content;

        Node(String t, Map<String,String> a, String c) {
            tag = t;
            attr = a;
            content = c;
        }
    }

    /** Esegue la visita dell'intero albero di parsing
     * @param visitor  visitatore invocato su ogni nodo dell'albero */
    void visit(Consumer<Node> visitor);

    /** Ritorna la lista (possibilmente vuota) dei links contenuti nella pagina
     * @return la lista dei links (mai null) */
    List<String> getLinks();

    /** Ritorna la lista (possibilmente vuota) dei nodi con lo specificato tag
     * @param tag  un nome di tag
     * @return la lista dei nodi con il dato tag (mai null) */
    List<Node> getByTag(String tag);
}

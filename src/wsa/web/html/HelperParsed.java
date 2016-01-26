package wsa.web.html;

import java.util.*;
import java.util.function.Consumer;
/**
 * Created by Takeru on 31/05/15.
 */
public class HelperParsed implements Parsed {
    /**
     * Crea un nuovo nuovo
     * @param t il nome del tag
     * @param a Gli attributi dell'elemento o null
     * @param c Contesto
     * @return Ritorna un nodo
     */
    public Node creanodo(String t, Map<String,String> a, String c)
    {
        setta.add(new Node(t,a,c));
        return new Node(t,a,c);
    }

    public Set<Node> setta = new HashSet<Node>();

    public Set<Node> getSetta(){
        return setta;
    }

    /**
     * Esegue la visita dell'intero albero di parsing
     *
     * @param visitor visitatore invocato su ogni nodo dell'albero
     */
    @Override
    public void visit(Consumer<Node> visitor) {
        for(Node n: setta){
            visitor.accept(n);
        }
    }

    /**
     * Ritorna la lista (possibilmente vuota) dei links contenuti nella pagina
     *
     * @return la lista dei links (mai null)
     */
    @Override
    public List<String> getLinks() {
        Set<String> ls = new HashSet<String>();
        for(Node nn:getByTag("A"))
            for(Map.Entry<String,String> jo: nn.attr.entrySet())
                if (jo.getKey().contains("href"))
                    ls.add(jo.getValue());
        return new ArrayList<>(ls);
    }

    /**
     * Ritorna la lista (possibilmente vuota) dei nodi con lo specificato tag
     *
     * @param tag un nome di tag
     * @return la lista dei nodi con il dato tag (mai null)
     */
    @Override
    public List<Node> getByTag(String tag) {
        List<Node> ln = new ArrayList<>();
        for(Node nn:getSetta()){
            if(nn.tag.equals(tag)){
                ln.add(nn);
            }
        }
        return ln;
    }

}

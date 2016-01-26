package wsa.web.html;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Una classe di parsing che rispetta le specifiche dell'interfaccia {@link Parsed}.
 * È un'implementazione considerata stabile, da test si evince una maggiore velocità
 * rispetto ad una versione iterativa.
 */
public class Parsing implements Parsed {

    private CreateTree Tree;

    /**
     * Crea un oggetto Parsing partendo da un documento dom, di tipo {@link Document}, questo
     * document non può essere usato al di fuori del javafx thread.
     * @param doc Il document da parsare.
     */
    public Parsing(Document doc) {
        Tree = new CreateTree(doc);
    }

    @Override
    public void visit(Consumer<Node> visitor) {
        TrueVisit(Tree, visitor);
    }

    @Override
    public List<String> getLinks() {
        List<String> Links = new ArrayList<>();
        TrueVisit(Tree, nodo-> {
            if ("a".equalsIgnoreCase(nodo.tag)) {
                Links.addAll(nodo.attr.keySet().stream()
                        .filter("href"::equalsIgnoreCase)
                        .map(nodo.attr::get)
                        .collect(Collectors.toList()));
            }
        });
        return Links;
    }

    @Override
    public List<Node> getByTag(String tag) {
        List<Node> NodesWithSuchTag = new ArrayList<>();
        TrueVisit(Tree, n ->{
            if (tag.equalsIgnoreCase(n.tag))        //Ricontrollare l'equalsIgnoreCase
                NodesWithSuchTag.add(n);
        });
        return NodesWithSuchTag;
    }

    /**
     * Prende un nodo e ne ottiene il tag.
     * @param nodo Il nodo da valutare.
     * @return La stringa che rappresenta il nome del nodo.
     */
    private String GimmeTag(org.w3c.dom.Node nodo) {                      //Utile per la creazione, trova il TAG del nodo, se è un nodo
        if (nodo.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
            return nodo.getNodeName();
        else
            return null;
    }

    /**
     * Prende un nodo e ne ottiene il tipo.
     * @param nodo Il nodo da valutare.
     * @return La stringa che rappresenta il tipo del nodo.
     */
    private String GimmeCont(org.w3c.dom.Node nodo){                   //Utile per la creazione, trova il contenuto del nodo, se è un nodo
        if (nodo.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
            return null;
        else
            return nodo.getNodeValue();
    }

    /**
     * Prende un nodo e ne ottiene la mappa degli attributi.
     * @param nodo Il nodo da valutare.
     * @return Una mappa degli attributi del nodo.
     */
    private Map<String, String> GimmeAttr(org.w3c.dom.Node nodo) {          //Utile per la creazione, trova gli attributi del nodo, se è un nodo
        if (nodo.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
            return null;
        Map<String,String> map = new HashMap<>();
        NamedNodeMap attributi = nodo.getAttributes();
        for (int cont=0; cont<attributi.getLength(); cont++) {
            map.put(attributi.item(cont).getNodeName(), attributi.item(cont).getNodeValue());
        }
        return map;
    }

    /**
     * Esegue una visita ricorsiva dell'albero.
     * @param albero L'albero da visitare.
     * @param consumatore Il consumer, non null, da accettare.
     */
    private void TrueVisit(CreateTree albero,Consumer<Node> consumatore)
    {
        if (albero == null || consumatore == null) return;
        consumatore.accept(albero);
        for(CreateTree figlio:albero.Figli)                         //Controllare se fosse necessario il -1, rischio che mi perda un figlio per strada.
        {
            TrueVisit(figlio, consumatore);                        //Ripete con il sottonodo
        }
    }

    /**
     * Classe che estende {@link wsa.web.html.Parsed.Node} aggiungendo una lista dei figli.
     * La domanda vera è: Perché {@link wsa.web.html.Parsed.Node} non esce di default con questa funzione?
     * Mistero? Io non credo.
     */
    private class CreateTree extends Node {

        CreateTree(org.w3c.dom.Node entra)                                 //Crea un nuovo albero da un Elemento(org.w3c.dom.Node)
        {
            super(GimmeTag(entra),GimmeAttr(entra),GimmeCont(entra));
            Figli = new ArrayList<>();
            for (int cont=0; cont<entra.getChildNodes().getLength(); cont++) {
                Figli.add(new CreateTree(entra.getChildNodes().item(cont)));     //Aggiunge ai .Figli de Tree un nuovo CreateTree(albero dello stesso tipo)
            }
        }

        List<CreateTree> Figli;                                 //Vedere se con un vettore sarebbe più veloce.
    }


}

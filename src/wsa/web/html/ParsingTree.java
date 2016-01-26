package wsa.web.html;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Created by Giuseppe on 5/21/2015. */

/** L'albero composto da oggetti della classe {@link wsa.web.html.Parsed.Node}.*/
public class ParsingTree implements Parsed{

    private AtomicReference<Tree> tree = new AtomicReference<>(); //albero di parsing atomico
    private AtomicReference<List<String>> links = new AtomicReference<>(); // lista atomica di stringhe, che sono indirizzi a pagine web
    private AtomicReference<List<Node>> nodeTags = new AtomicReference<>(); // lista atomica di oggi Node


    /** Classe annidata che estende {@link wsa.web.html.Parsed.Node},
     * serve per creare oggetti della classe annidata nell'interfaccia
     * {@link Parsed} per comporre l'albero di parsing
     */
    private class Tree extends Node{

        private List<Tree> nodes;

        Tree(org.w3c.dom.Node node) {
            super(tag(node), attr(node), content(node));
            NodeList list = node.getChildNodes();
            nodes = new ArrayList<>();
            for (int i = 0 ; i < list.getLength() ; i++){
                org.w3c.dom.Node child = list.item(i);
                nodes.add(new Tree(child));
            }
        }
    }

    /**
     * COSTRUTTORE
     * @param document da cui crea l'albero di parsing usando la classe {@link wsa.web.html.ParsingTree.Tree}
     */
    public ParsingTree(org.w3c.dom.Node document){
        nodeTags.set(new ArrayList<>()); // crea la lista per nodeTags
        links.set(new ArrayList<>()); // crea la lista per links
        tree.set( new Tree(document) ); // crea l'albero per tree
        init(); // riempe nodeTags e links con gli oggetti opportuni
    }


    private void init(){

        // aggiunge gli indirizzi nella lista di String, links, utile per getLinks
        visit(node -> {
            if (node.tag != null) {
                if (node.tag.equalsIgnoreCase("a")) {
                    links.get().addAll(node.attr.keySet().stream().
                            filter("href"::equalsIgnoreCase).map(node.attr::get).collect(Collectors.toList()));
                }
            }
        });

        //aggiunge node nella lista di node, Nodetags, utile per getByTag
        visit(node -> { if (node.tag != null) nodeTags.get().add(node); });
    }

/** usato per dei test

    @Override
    public String toString(){
        visit( node -> {
            if (node.tag == null)
                System.out.println("CONTENT --- >  " + node.content);
            else
                System.out.println( node.tag + " " + node.attr.toString());
        });
        return null;
    }
*/


    /** Accede al tag del nodo se c'è
     * @param node nodo per cui prendere il suo tag
     * @return il tag dell'elemento o null
     */
    private String tag(org.w3c.dom.Node node){
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
            //return node.toString();
            return node.getNodeName();
        return null;
    }

    /** Accede agli attributi del nodo se ci sono
     * @param node nodo per cui prendere i suoi attributi
     * @return gli attributi dell'elemento o null
     */
    private Map<String, String> attr(org.w3c.dom.Node node){
        try {
            NamedNodeMap namedNodeMap = node.getAttributes();
            Map<String, String> attrMap = new HashMap<>();

            for (int i = 0; i < namedNodeMap.getLength(); i++) {
                attrMap.put(namedNodeMap.item(i).getNodeName(), namedNodeMap.item(i).getNodeValue());
            }

            return attrMap;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Accede al testo dentro un nodo se c'è, altrimenti è null
     * @param node nodo per cui prendere il suo contenuto
     * @return il contenuto del nodo, altrimenti null*/
    private String content(org.w3c.dom.Node node){
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
            return null;
        else {
            //return node.getTextContent();
            return node.getNodeValue();
        }
    }


    /** Esegue la visita dell'intero albero di parsing
     * @param visitor  visitatore invocato su ogni nodo dell'albero */
    @Override
    public void visit(Consumer<Node> visitor) {
        ricerca(tree.get(), visitor);
    }

    private void ricerca(Tree t, Consumer<Node> consumer) {
        consumer.accept(t);
        for (Tree tree : t.nodes)
            ricerca(tree, consumer);
    }



    /** Ritorna la lista (possibilmente vuota) dei links contenuti nella pagina
     * @return la lista dei links (mai null) */
    @Override
    public List<String> getLinks() {
        return links.get();
    }


    /** Ritorna la lista (possibilmente vuota) dei nodi con lo specificato tag
     * @param tag  un nome di tag
     * @return la lista dei nodi con il dato tag (mai null) */
    @Override
    public List<Node> getByTag(String tag) {
        return nodeTags.get().stream().filter(n -> n.tag.equalsIgnoreCase(tag)).collect(Collectors.toList());
    }
}

package wsa.gui;


import lombok.NonNull;
import wsa.session.Page;

import java.util.List;

/**
 * Created by lorenzo on 2/13/16.
 * Astrazione per grafi di Page.
 */
//TODO forse non va in questo package.
public class Graphs {
    public Graphs(){}

    public Graphs(@NonNull List<Page> from){

    }

    public void AddAndLink(@NonNull Page pag){
    }

    public void Add(@NonNull Page pag){}

    public void Link(@NonNull Page pag, Page arr){}

}

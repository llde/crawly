package wsa;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by gaamda on 15/11/15.
 *
 * Settings del programma.
 */
public class Settings {
    static class Setting{
        private String ID;
        private String Type;  //b boolean, i integer, f float, s String, c char,
        private String Value;
        Setting(String id, String type, String  value){
            ID  = id;
            Type = type;
            Value = value;
        }
    }
    public static HashMap<String, Setting> settings = new HashMap<>();
    public static Integer RES_GRABBER_MILLIS = 1000;
    public final static SimpleBooleanProperty CR_FOLLOW = new SimpleBooleanProperty(true);
    public static boolean RUN_WITH_LOGO = false;
    //public static Boolean CR_FOLLOW = true

    public static ObjectProperty<Color> CR_PTD = new SimpleObjectProperty<>(Color.VIOLET);  //Colore default per puntati
    public static ObjectProperty<Color> CR_PTR = new SimpleObjectProperty<>(Color.YELLOW);  //Colore default per i puntati

    /*gradiente di colore default cotruito sui colori di default*/
    private static ObjectProperty<Stop[]> stops = new SimpleObjectProperty<>(new Stop[]{
            new Stop(0, CR_PTR.get()),
            new Stop(1, CR_PTD.get())
    });
    public static ObjectProperty<LinearGradient> CR_PTDandPTR = new SimpleObjectProperty<>(
            new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops.get())
    );

    private static boolean strictCrawling = true;

    //O trovare un'altro modo per filtrare, stavolta per whitelist, con metodi per considerare il fragment senza estensione

    public static boolean getStrictCrawling(){return strictCrawling;}


}

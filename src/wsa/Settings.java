package wsa;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
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
        private String ID; //first letter is type: b boolean, i integer, f float, s String, c char,
        private String Value;
        Setting(String id, String  value){
            ID  = id;
            Value = value;
        }
    }
    private static File file = new File("./Configure.ini");
    @Accessors(fluent=true) @Getter(lazy = true) private static final Settings config = new Settings(file);
    public  HashMap<String, Setting> settings = new HashMap<>();
    public  Integer RES_GRABBER_MILLIS = 1000;
    public final  SimpleBooleanProperty CR_FOLLOW = new SimpleBooleanProperty(true);
    public  boolean RUN_WITH_LOGO = false;
    //public static Boolean CR_FOLLOW = true

    public  ObjectProperty<Color> CR_PTD = new SimpleObjectProperty<>(Color.VIOLET);  //Colore default per puntati
    public  ObjectProperty<Color> CR_PTR = new SimpleObjectProperty<>(Color.YELLOW);  //Colore default per i puntati

    /*gradiente di colore default cotruito sui colori di default*/
    private  ObjectProperty<Stop[]> stops = new SimpleObjectProperty<>(new Stop[]{
            new Stop(0, CR_PTR.get()),
            new Stop(1, CR_PTD.get())
    });
    public  ObjectProperty<LinearGradient> CR_PTDandPTR = new SimpleObjectProperty<>(
            new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops.get())
    );

    private Settings(File conf){
        if(conf.exists()) {
            try (BufferedReader file = Files.newBufferedReader(conf.toPath())){
                file.lines();
                //DO other things;
                return;
            } catch (Exception e) {}
        }
        //Apply defaults

    }

    public boolean save(){return false;}

    public <T> T getSetting(String id){
        return null;
    }

    public void addSetting(Setting setting ){

    }



}

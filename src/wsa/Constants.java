package wsa;

import java.util.Random;

/**
 * Created by gaamda on 14/11/15.
 *
 * Costanti globali per l'applicazione. Le costanti sono intese non modificabili ed accessibili da qualsiasi
 * oggetto presente all'interno dell'applicazione stessa.
 */
public class Constants {

    public enum grabberMillis {veloce, medio, lento}
    public static final String APPLICATION_NAME = "Crawly";
    public static final String APPLICATION_VERSION = "2.00 WIP";

    public static final String ERROR_GUIERROR = "Un errore nella generazione della GUI\n" +
            "impedisce il corretto funzionamento di " + APPLICATION_NAME + ".\n" +
            "Il programma terminerà.";

    public static final String[] illegalExtension = {".jpg" , ".pdf" , ".gif", ".mp3", ".mp4", ".flv" ,".avi", ".png" , ".bmp", ".css"};

    public static final String[] illegalProtocols = {"mailto:" , "apt://" , "magnet:?" , "telnet://"};

    private final static String[] strings = new String[]{
            "Kawabonga!",
            "The choosen one",
            "La mitica numero uno",
            "Circa 6000 righe di codice\n" +
                    "tutte buggate",
            "Crawl from Italy with love",
            "Lo stato dell'arte secondo\n" +
                    "dei procioni amanti del té",
            "I creatori hanno bevuto caffè,\n" +
                    "qualità java",
            "It's a trap!",
            "May the Force be\n" +
                    "with you.",
            "The cake is a lie!",
            "C'è un virus sul tuo PC,\n" +
                    "sei tu",
            "Problem exists between chair \n and keyboard",
            "Goodbye blue skies",
            "Ma che te lo dico a fare?!",
            "The JVM said me \"NO\"",
            "Non userò LWJGL come minecraft,\n" +
                    "giuro su ogni mio bit!",
            "Ave Sithis!"
    };

    private static int previousChoice = 0;

    public static String getRandomString(){
        int choice = new Random().nextInt(strings.length);
        while (choice == previousChoice){
            choice = new Random().nextInt(strings.length);
        }
        previousChoice = choice;
        try {
            Settings.config().RUN_WITH_LOGO = strings[choice].equals("Non userò LWJGL come minecraft,\n" +
                    "giuro su ogni mio bit!")
                    || strings[choice].equals("The JVM said me \"NO\"");
            return strings[choice];
        }catch (Exception ex){
            return strings[0];
        }
    }
}

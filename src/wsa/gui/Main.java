package wsa.gui;

import javafx.application.Application;
import javafx.stage.Stage;
import wsa.web.HelperLoader;
import wsa.web.WebFactory;

/**
 * Created by gaamda on 03/11/2015.
 *
 * Questa classe esegue l'intero programma lanciandolo. Per far partire Crawly lanciare il metodo main.
 * Per utilizzare solo il core dell'applicazione, riferirsi al package wsa.
 */
public class Main extends Application {

    /**
     * Fa partire Crawly con la finestra principale.
     */
    public static void main(String[] args){
        //WebFactory.setLoaderFactory(() -> new HelperLoader());
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        MainFrame main = MainFrame.getMainFrame();
        main.show();
    }

}

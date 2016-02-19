package wsa.plugin.charts;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import lombok.NonNull;
import wsa.API.Wrap;
import wsa.session.Page;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gaamda on 19/02/16.
 *
 * Representing a PieChart that organize data in class of size n.
 * Default value may change, set the setting in each case.
 */
public class ModulePieChart extends ChartPlugin<URI> {

    public enum linksValue {POINTER, POINTED, EXTERNALS, INTERNALS}

    private final ObservableList<Page> dataset;
    private final PieChart pieChart = new PieChart();

    // Settings
    private final Wrap<String, Integer> module = new Wrap<>(Integer.class.toGenericString(), 5);
    private final Wrap<String, linksValue> links = new Wrap<>(linksValue.class.toGenericString(), linksValue.POINTER);
    private final Wrap<String, Boolean> animated = new Wrap<>(Boolean.class.toGenericString(), false);
    private final Wrap<String, Boolean> legend = new Wrap<>(Boolean.class.toGenericString(), true);
    private final Wrap<String, Boolean> alwaysOnTop = new Wrap<>(Boolean.class.toGenericString(), true);

    public ModulePieChart(@NonNull ObservableList<Page> dataset) {
        super(new HashMap<>(), dataset);
        module.val.addListener(param -> this.calculate());
        super.properties.put("module", module);
        super.properties.put("linkvalue", links);
        super.properties.put("legend", legend);
        super.properties.put("alwaysontop", alwaysOnTop); // Affect newly created window
        this.dataset = dataset;

        Platform.runLater(() -> pieChart.setAnimated(animated.val.get()));

        dataset.addListener((InvalidationListener) observable -> {
            setChartData();
        });
        pieChart.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                Platform.runLater(this::showDetails);
            }
        });

        module.val.addListener(observable -> {
            setChartData();
        });
        links.val.addListener(observable -> {
            setChartData();
        });
        animated.val.addListener(observable -> {
            Platform.runLater(() -> pieChart.setAnimated(animated.val.get()));
        });
        legend.val.addListener(observable -> {
            Platform.runLater(() -> pieChart.setLegendVisible(legend.val.get()));
        });
    }

    @Override
    public Chart getGraph() throws Exception {
        return pieChart;
    }

    @Override
    public ObservableList<URI> getDatas() {
        return null;
    }

    private void setChartData(){
        Platform.runLater(() -> {
            pieChart.getData().clear();
            pieChart.getData().addAll(createPieChartData(calculate()));
        });
    }

    private ObservableList<PieChart.Data> createPieChartData(Map<Integer, Set<URI>> values){
        ObservableList<PieChart.Data> createdList = FXCollections.observableArrayList();
        values.forEach((k,v) -> {
            createdList.add(new PieChart.Data("from " + module.val.get()*k + " to " + ((module.val.get()*k + module.val.get()) -1), v.size()));
        });
        return createdList;
    }

    private Map<Integer, Set<URI>> calculate(){
        Map<Integer, Set<URI>> values = new HashMap<>();
        for (Page pg : this.dataset){
            int val = getURI(pg, links.val.get()).size() / module.val.get();
            if (values.containsKey(val)){
                values.get(val).add(pg.getURI());
            }else{
                Set<URI> toAdd = new HashSet<>();
                toAdd.add(pg.getURI());
                values.put(val, toAdd);
            }
        }
        return values;
    }

    private Set<URI> getURI(Page pg, linksValue value){
        switch (value){
            case POINTER:
                return pg.getPtr();
            case POINTED:
                return pg.getPtd();
            case EXTERNALS:
                return pg.getUscenti();
            case INTERNALS:
                return pg.getEntranti();
            default:
                return new HashSet<>();
        }
    }

    private void showDetails(){
        TableView<PieChart.Data> table = new TableView<>();
        TableColumn<PieChart.Data, String> classColumn = new TableColumn<>("Links");
        TableColumn<PieChart.Data, Integer> amountColumn = new TableColumn<>("Pages");
        table.getColumns().add(classColumn);
        table.getColumns().add(amountColumn);

        classColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        amountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>((int)param.getValue().getPieValue()));

        table.setItems(this.pieChart.getData());

        Stage stg = new Stage();
        stg.setTitle(links.val.get().toString());

        stg.setScene(new Scene(table));

        stg.setAlwaysOnTop(alwaysOnTop.val.get());
        stg.show();
    }
}

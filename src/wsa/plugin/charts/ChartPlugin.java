package wsa.plugin.charts;

import javafx.collections.ObservableList;
import javafx.scene.chart.Chart;
import lombok.NonNull;
import lombok.Synchronized;
import wsa.API.Wrap;
import wsa.plugin.Plugin;
import wsa.session.Page;

import java.util.Collections;
import java.util.Map;

/**
 * Created by gaamda on 19/02/16.
 *
 * Represent a plugin that hold a Chart. The chartPlugin need a list of Pages in the constructor.
 * It will be assumed as default.
 * With this data, the Chart plugin can give his visualizations to the user.
 *
 * @since 1.0 WIP
 * @author gaamda
 */
public abstract class ChartPlugin<T> implements Plugin {

    protected final Map<String, Wrap> properties;
    protected final ObservableList<Page> dataset;

    public ChartPlugin(@NonNull  Map<String, Wrap> settings, @NonNull ObservableList<Page> dataset){
        this.properties = settings;
        this.dataset = dataset;
    }

    /**
     * Return the graphic content of the plugin. It will be inserted as-is in the
     * graph panel. Use in a try catch cycle.
     * @return Graphic Node.
     */
    public abstract Chart getGraph() throws Exception;

    /**
     * Return a list of data hold by the chart.
     * @return List of data.
     */
    public abstract ObservableList<T> getDatas();

    /**
     * Get the map of properties.
     * Assuming the lowerCase mode for keys.
     * @return List of settings.
     */
    public Map<String, Wrap> getSettings(){
        return Collections.unmodifiableMap(this.properties);
    }

    /**
     * Set (if exist) the property with the given (if valid) value.
     * Assuming the lowerCase mode for keys.
     * @param name Property to search by.
     * @param value Value to assign.
     * @return value if set, null otherwise.
     */
    public <G> G setSettings(String name, G value){
        name = name.toLowerCase();
        if (!properties.containsKey(name)) return null;
        if (properties.get(name).val.get().getClass().equals(value.getClass())){
            ((Wrap<String, G>) properties.get(name)).val.set(value);
            return value;
        }
        return null;
    }

}

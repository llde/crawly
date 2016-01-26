package wsa.web;

import java.net.URI;
import java.nio.file.Path;

/**
 * Created by Lorenzo on 26/01/2016.
 */
public interface SiteCrawlerFactory {
    SiteCrawler newInstance(URI dom, Path dir);
}

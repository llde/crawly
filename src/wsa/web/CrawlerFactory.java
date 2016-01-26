package wsa.web;

import java.net.URI;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Created by Lorenzo on 26/01/2016.
 */
public interface CrawlerFactory {

    Crawler newInstance(Collection<URI> loaded,
                        Collection<URI> toLoad,
                        Collection<URI> errs,
                        Predicate<URI> pageLink);
}

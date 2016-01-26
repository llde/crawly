package wsa.web.html;


import org.w3c.dom.Document;

/**
 * Created by Lorenzo on 26/01/2016.
 */
public interface ParsedFactory {
    Parsed newInstance(Document doc);
}

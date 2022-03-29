package domn8.nodes;

import org.apache.logging.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.logging.log4j.LogManager.getLogger;

public abstract class Node {
    final Logger log = getLogger("DomNode");

    protected static Element newNode(final String tag) {
        return DocumentHelper.createElement(tag);
    }

    public abstract Element render();

    protected abstract Element build();

    protected Element build(final Function<Element, Element> modifier) {
        return modifier.apply(build());
    }

    // TODO Remove?
    protected Element _build(final Consumer<Element> modifier) {
        final Element el = build();
        modifier.accept(el);
        return el;
    }
}

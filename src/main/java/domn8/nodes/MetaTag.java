package domn8.nodes;

import org.dom4j.Element;

import java.util.Collections;
import java.util.List;

import static domn8.nodes.MetaTag.MetaConfig;

public class MetaTag extends DomNode<MetaConfig> {

    protected MetaTag(MetaConfig config, List<DomNode<?>> children) {
        super(config, children);
    }

    public static MetaTag meta(final String name, final String value) {
        return new MetaTag(new MetaConfig().additionalAttribute(name, value), Collections.emptyList());
    }

    public MetaTag add(final String name, final String value) {
        this.config.additionalAttribute(name, value);
        return this;
    }

    @Override
    public Element render() {
        return build();
    }

    public static class MetaConfig extends Config {

        public MetaConfig additionalAttribute(final String name, final String value) {
            return (MetaConfig) super.additionalAttribute(name, value);
        }

        @Override
        public String node() {
            return "meta";
        }
    }
}

package domn8.nodes.elements;

import domn8.nodes.DomNode;
import domn8.styles.CSS;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

import static domn8.nodes.elements.HeaderEl.HeaderConfig;

public class HeaderEl extends BodyEl<HeaderConfig> {
    HeaderEl(HeaderConfig config, List<DomNode<?>> children) {
        super(config, children);
    }

    public static HeaderEl headerEl(final HeaderConfig config) {
        return new HeaderEl(config, new ArrayList<>());
    }

    public static HeaderConfig headerConfig() {
        return new HeaderConfig();
    }

    @Override
    public Element render() {
        return build(el -> {
            el.setText(config.text);
            return el;
        });
    }

    public static class HeaderConfig extends ElConfig {
        private int level;
        private String text;

        public HeaderConfig level(final int level) {
            this.level = level;
            return this;
        }

        public HeaderConfig text(final String text) {
            this.text = text;
            return this;
        }

        @Override
        public String node() {
            return String.format("h%d", level);
        }

        public HeaderConfig classNames(final String... classes) {
            return (HeaderConfig) super.classNames(classes);
        }

        public HeaderConfig styles(final CSS css) {
            return (HeaderConfig) super.styles(css);
        }
    }
}

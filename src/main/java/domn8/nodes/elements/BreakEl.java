package domn8.nodes.elements;

import domn8.styles.CSS;
import org.dom4j.Element;

import java.util.ArrayList;

import static domn8.nodes.elements.BreakEl.BreakConfig;

public class BreakEl extends BodyEl<BreakConfig> {

    BreakEl(BreakConfig config) {
        super(config, new ArrayList<>());
    }

    public static BreakEl breakEl() {
        return new BreakEl(new BreakConfig());
    }

    @Override
    public Element render() {
        return _build(el -> {

        });
    }

    public static class BreakConfig extends ElConfig {
        @Override
        public String node() {
            return "br";
        }

        @Override
        public BreakConfig styles(CSS styles) {
            return (BreakConfig) super.styles(styles);
        }
    }
}

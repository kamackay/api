package domn8.nodes;

import domn8.DOMn8;
import org.dom4j.Element;

import java.util.*;

import static domn8.nodes.HeadNode.HeadConfig;
import static java.util.stream.Collectors.toList;

public class HeadNode extends DomNode<HeadConfig> {

    HeadNode(HeadConfig config, final List<DomNode<?>> children) {
        super(config, children);
    }

    public static HeadNode head(final HeadConfig config, List<DomNode<?>> children) {
        final List<DomNode<?>> childList = config.stylesheets
                .stream()
                .map(StylesheetNode::stylesheetNode)
                .collect(toList());
        childList.addAll(children);
        return new HeadNode(config, childList);
    }

    public static HeadNode head(final HeadConfig config) {
        return head(config, Collections.emptyList());
    }

    @Override
    public Element render() {
        return _build(el -> {
            final Element titleEl = newNode("title");
            titleEl.setText(config.title);
            el.add(titleEl);

            final Element icon = newNode("link");
            List.of(
                    DOMn8.makeAttr("rel", "shortcut icon"),
                    DOMn8.makeAttr("type", "image/x-icon"),
                    DOMn8.makeAttr("href", config.iconUrl)
            ).forEach(el::add);
            el.add(icon);
        });
    }

    public static class HeadConfig extends Config {
        private final List<String> stylesheets = new ArrayList<>();
        private final List<String> scripts = new ArrayList<>();
        private String title;
        private String iconUrl;

        public HeadConfig stylesheets(final Collection<String> sheets) {
            this.stylesheets.clear();
            this.stylesheets.addAll(sheets);
            return this;
        }

        public HeadConfig scripts(final Collection<String> scripts) {
            this.scripts.clear();
            this.scripts.addAll(scripts);
            return this;
        }

        public HeadConfig scripts(String... scripts) {
            return this.scripts(Arrays.asList(scripts));
        }

        public HeadConfig title(final String title) {
            this.title = title;
            return this;
        }

        public HeadConfig iconUrl(final String url) {
            this.iconUrl = url;
            return this;
        }

        public HeadConfig stylesheets(String... sheets) {
            return this.stylesheets(Arrays.asList(sheets));
        }

        @Override
        public String node() {
            return "head";
        }
    }

}

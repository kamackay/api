package domn8.nodes.elements;

import domn8.styles.CSS;
import lombok.Getter;
import org.dom4j.Element;

import static domn8.nodes.elements.LinkEl.LinkConfig;
import static domn8.util.Utils.listOf;

public class LinkEl extends BodyEl<LinkConfig> {

  LinkEl(LinkConfig config) {
    super(config, listOf());
  }

  public static LinkEl linkEl(final LinkConfig config) {
    return new LinkEl(config);
  }

  @Override
  public Element render() {
    return _build(el -> {
      el.setText(config.getText());
      el.addAttribute("href", config.getUrl());
      if (config.isNewTab()) {
        el.addAttribute("target", "_blank");
      }
    });
  }

  public static class LinkConfig extends ElConfig {
    @Getter
    private String text;
    @Getter
    private String url;
    @Getter
    private boolean newTab;

    public LinkConfig text(final String text) {
      this.text = text;
      return this;
    }

    public LinkConfig url(final String url) {
      this.url = url;
      return this;
    }

    public LinkConfig setNewTab(final boolean value) {
      this.newTab = value;
      return this;
    }

    public LinkConfig fontSize(final int size) {
      return this.styles(CSS.css()
          .setValue("font-size",
              String.format("%dpx", size)));
    }

    @Override
    public String node() {
      return "a";
    }

    @Override
    public LinkConfig styles(CSS styles) {
      return (LinkConfig) super.styles(styles);
    }
  }
}

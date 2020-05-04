package io.keithm.domn8.nodes.elements;

import io.keithm.domn8.styles.CSS;
import lombok.Getter;
import org.dom4j.Element;

import static com.google.common.collect.Lists.newArrayList;
import static io.keithm.domn8.nodes.elements.LinkEl.LinkConfig;

public class LinkEl extends BodyEl<LinkConfig> {

  LinkEl(LinkConfig config) {
    super(config, newArrayList());
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

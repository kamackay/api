package com.keithmackay.api.domn8.nodes;

import lombok.Getter;
import org.dom4j.Element;

import static com.google.common.collect.Lists.newArrayList;
import static com.keithmackay.api.domn8.nodes.StylesheetNode.StylesheetConfig;


public class StylesheetNode extends DomNode<StylesheetConfig> {

  StylesheetNode(StylesheetConfig config) {
    super(config, newArrayList());
  }

  public static StylesheetNode stylesheetNode(final String url) {
    return new StylesheetNode(new StylesheetConfig()
        .url(url));
  }

  @Override
  public Element render() {
    return build(el -> {
      el.addAttribute("rel", "stylesheet");
      el.addAttribute("href", config.url);
      return el;
    });
  }

  public static class StylesheetConfig extends Config {
    @Getter
    private String url;

    public String node() {
      return "link";
    }

    public StylesheetConfig url(final String url) {
      this.url = url;
      return this;
    }
  }
}

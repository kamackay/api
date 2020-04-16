package com.keithmackay.api.domn8.nodes;

import lombok.Getter;
import org.dom4j.Element;

import static com.google.common.collect.Lists.newArrayList;
import static com.keithmackay.api.domn8.nodes.HtmlPage.HtmlConfig;

public class HtmlPage extends DomNode<HtmlConfig> {

  public HtmlPage(HtmlConfig config, final HeadNode header, final HtmlBody body) {
    super(config, newArrayList(header, body));
  }

  @Override
  public Element render() {
    return build(el -> {
      el.attributeValue("lang", "en");
      return el;
    });
  }

  public static class HtmlConfig extends Config {

    @Override
    public String node() {
      return "html";
    }

    @Getter
    private String title;

    public HtmlConfig title(final String title) {
      this.title = title;
      return this;
    }
  }
}

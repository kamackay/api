package io.keithm.domn8.nodes;

import org.dom4j.Element;

import java.util.List;

import static io.keithm.domn8.nodes.HtmlBody.BodyConfig;

public class HtmlBody extends DomNode<BodyConfig> {

  HtmlBody(BodyConfig config, final List<DomNode<?>> children) {
    super(config, children);
  }

  public static HtmlBody body(final BodyConfig config, final List<DomNode<?>> children) {
    return new HtmlBody(config, children);
  }

  @Override
  public Element render() {
    return build(el -> {
      el.addAttribute("id", "body");
      return el;
    });
  }

  public static class BodyConfig extends Config {

    @Override
    public String node() {
      return "body";
    }
  }
}

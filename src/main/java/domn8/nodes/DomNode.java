package domn8.nodes;

import lombok.Getter;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

import static domn8.DOMn8.makeAttr;

public abstract class DomNode<C extends DomNode.Config> extends Node {

  protected final C config;
  List<DomNode<?>> children = new ArrayList<>();

  protected DomNode(final C config, List<DomNode<?>> children) {
    this.config = config;
    this.children.addAll(children);
    config.configDone();
  }

  @Override
  protected Element build() {
    final Element el = newNode(config.node());
    this.config.getAttributes().forEach(el::add);
    children.forEach(child -> el.add(child.render()));
    return el;
  }

  public static abstract class Config {
    public abstract String node();

    @Getter
    private final List<Attribute> attributes = new ArrayList<>();

    public Config additionalAttribute(final String name, final String value) {
      this.attributes.add(makeAttr(name, value));
      return this;
    }

    public void configDone() {
      // No-op
    }
  }
}

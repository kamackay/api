package com.keithmackay.api.domn8.nodes.elements;

import com.keithmackay.api.domn8.nodes.DomNode;
import com.keithmackay.api.domn8.styles.CSS;
import org.dom4j.Element;

import java.util.List;

import static com.keithmackay.api.domn8.nodes.elements.DivEl.*;

public class DivEl extends BodyEl<DivConfig> {

  DivEl(DivConfig config, List<DomNode<?>> children) {
    super(config, children);
  }

  public static DivEl divEl(final DivConfig config, List<DomNode<?>> children) {
    return new DivEl(config, children);
  }

  public static DivConfig divConfig() {
    return new DivConfig();
  }

  @Override
  public Element render() {
    return build(el -> el);
  }

  public static final class DivConfig extends ElConfig {

    public DivConfig styles(final CSS css) {
      return (DivConfig) super.styles(css);
    }

    public DivConfig classNames(final String...classes) {
      return (DivConfig) super.classNames(classes);
    }

    @Override
    public String node() {
      return "div";
    }
  }
}

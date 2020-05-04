package io.keithm.domn8.nodes.elements;

import io.keithm.domn8.styles.CSS;
import org.dom4j.Element;

import static com.google.common.collect.Lists.newArrayList;
import static io.keithm.domn8.nodes.elements.BreakEl.BreakConfig;

public class BreakEl extends BodyEl<BreakConfig> {

  BreakEl(BreakConfig config) {
    super(config, newArrayList());
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

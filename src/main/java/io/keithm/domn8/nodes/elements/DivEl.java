package io.keithm.domn8.nodes.elements;

import io.keithm.domn8.nodes.DomNode;
import io.keithm.domn8.styles.CSS;
import lombok.Getter;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

import static io.keithm.domn8.nodes.elements.DivEl.DivConfig;

public class DivEl extends BodyEl<DivConfig> {

  DivEl(DivConfig config, List<DomNode<?>> children) {
    super(config, children);
  }

  public static DivEl divEl(final DivConfig config, List<DomNode<?>> children) {
    return new DivEl(config, children);
  }

  public static DivEl divEl(final DivConfig config) {
    return new DivEl(config, new ArrayList<>());
  }

  public static DivConfig divConfig() {
    return new DivConfig();
  }

  @Override
  public Element render() {
    return build(el -> {
      if (this.config.getContent() != null) {
        el.setData(this.config.getContent());
      }
      return el;
    });
  }

  public static final class DivConfig extends ElConfig {

    @Getter
    private String content = null;

    public DivConfig content(final String content) {
      this.content = content;
      return this;
    }


    public DivConfig styles(final CSS css) {
      return (DivConfig) super.styles(css);
    }

    public DivConfig classNames(final String... classes) {
      return (DivConfig) super.classNames(classes);
    }

    @Override
    public String node() {
      return "div";
    }
  }
}

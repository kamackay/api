package io.keithm.domn8.nodes.elements;

import io.keithm.domn8.nodes.DomNode;
import io.keithm.domn8.styles.CSS;
import lombok.AccessLevel;
import lombok.Getter;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.keithm.domn8.nodes.elements.BodyEl.ElConfig;
import static java.util.function.Predicate.not;

public abstract class BodyEl<C extends ElConfig> extends DomNode<C> {

  BodyEl(final C config, final List<DomNode<?>> children) {
    super(config, children);
  }

  @Override
  protected Element build() {
    final Element el = super.build();
    Optional.ofNullable(config.getClassNames())
        .filter(not(List::isEmpty))
        .map(l -> String.join(" ", l))
        .ifPresent(str -> el.addAttribute("class", str));

    Optional.ofNullable(config.getCss())
        .filter(not(CSS::isEmpty))
        .map(CSS::print)
        .ifPresent(str -> el.addAttribute("style", str));

    return el;
  }

  public abstract static class ElConfig extends Config {
    @Getter(value = AccessLevel.PROTECTED)
    private final CSS css = CSS.css();
    @Getter(value = AccessLevel.PROTECTED)
    private final List<String> classNames = new ArrayList();

    public ElConfig styles(final CSS styles) {
      this.css.merge(styles);
      return this;
    }

    public ElConfig classNames(final List<String> classNames) {
      this.classNames.addAll(classNames);
      return this;
    }

    public ElConfig classNames(final String... classNames) {
      return classNames(Arrays.asList(classNames));
    }
  }
}

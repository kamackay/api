package io.keithm.domn8.nodes.elements;

import io.keithm.domn8.styles.CSS;
import lombok.Getter;
import org.dom4j.Element;

import static com.google.common.collect.Lists.newArrayList;
import static io.keithm.domn8.nodes.elements.CodeNode.CodeConfig;

public class CodeNode extends BodyEl<CodeConfig> {

  CodeNode(CodeConfig config) {
    super(config, newArrayList());
  }

  public static CodeNode codeEl(final String text) {
    return codeEl(new CodeConfig().text(text));
  }

  public static CodeNode codeEl(final CodeConfig config) {
    return new CodeNode(config);
  }

  @Override
  public Element render() {
    return _build(el -> {
      el.setText(config.getText());
    });
  }

  public static class CodeConfig extends ElConfig {
    @Getter
    private String text;

    public CodeConfig text(final String text) {
      this.text = text;
      return this;
    }

    public CodeConfig fontSize(final int size) {
      return this.styles(CSS.css()
          .setValue("font-size",
              String.format("%dpx", size)));
    }

    @Override
    public String node() {
      return "code";
    }

    @Override
    public CodeConfig styles(CSS styles) {
      return (CodeConfig) super.styles(styles);
    }
  }
}


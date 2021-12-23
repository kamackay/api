package domn8.nodes.elements;

import domn8.styles.CSS;
import lombok.Getter;
import org.dom4j.Element;

import static domn8.nodes.elements.TextNode.TextConfig;
import static domn8.util.Utils.listOf;

public class TextNode extends BodyEl<TextConfig> {

  TextNode(TextConfig config) {
    super(config, listOf());
  }

  public static TextNode textNode(final String text) {
    return textNode(new TextConfig().text(text));
  }

  public static TextNode textNode(final TextConfig config) {
    return new TextNode(config);
  }

  @Override
  public Element render() {
    return _build(el -> {
      el.setText(config.getText());
    });
  }

  public static class TextConfig extends ElConfig {
    @Getter
    private String text;

    public TextConfig text(final String text) {
      this.text = text;
      return this;
    }

    public TextConfig fontSize(final int size) {
      return this.styles(CSS.css()
          .setValue("font-size",
              String.format("%dpx", size)));
    }

    @Override
    public String node() {
      return "text";
    }

    @Override
    public TextConfig styles(CSS styles) {
      return (TextConfig) super.styles(styles);
    }
  }
}

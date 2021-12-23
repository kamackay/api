package domn8.nodes.elements;

import domn8.styles.CSS;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Base64;

import static domn8.nodes.elements.ImgNode.ImgConfig;

public class ImgNode extends BodyEl<ImgConfig> {

  ImgNode(ImgConfig config) {
    super(config, new ArrayList<>());
  }

  public static ImgNode imgNode(final ImgConfig config) {
    return new ImgNode(config);
  }

  @Override
  public Element render() {
    return _build(el -> {
      el.addAttribute("src", config.getSrc())
          .addAttribute("height", String.valueOf(config.getHeight()))
          .addAttribute("alt", config.getAlt());
    });
  }

  @Slf4j
  public static class ImgConfig extends ElConfig {
    @Getter
    private String src;
    @Getter
    private String alt;
    @Getter
    private int height;
    @Getter
    private boolean preRendered;

    public ImgConfig src(final String src) {
      this.src = src;
      return this;
    }

    public ImgConfig alt(final String alt) {
      this.alt = alt;
      return this;
    }

    public ImgConfig height(final int size) {
      this.height = size;
      return this;
    }

    public ImgConfig preRendered(final boolean preRendered) {
      this.preRendered = preRendered;
      return this;
    }

    @Override
    public void configDone() {
      if (this.isPreRendered() && this.getSrc() != null) {
        // Fetch URL
        try {
          final OkHttpClient client = new OkHttpClient();
          final Request request = new Request.Builder()
                  .url(this.getSrc())
                  .build();
          try (Response response = client.newCall(request).execute()) {
            this.src(String.format("data:%s;base64,%s",
                    response.header("Content-Type", "image/png"),
                    Base64.getEncoder().encodeToString(response.body().bytes())));
          }
        } catch (Exception e) {
          log.error("Error Pre-Rendering Img", e);
        }
      }
    }
    @Override
    public String node() {
      return "img";
    }

    @Override
    public ImgConfig styles(CSS styles) {
      return (ImgConfig) super.styles(styles);
    }
  }
}

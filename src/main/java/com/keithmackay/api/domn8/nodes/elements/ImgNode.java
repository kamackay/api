package com.keithmackay.api.domn8.nodes.elements;

import com.keithmackay.api.domn8.styles.CSS;
import khttp.responses.Response;
import lombok.Getter;
import org.dom4j.Element;

import java.util.Base64;

import static com.google.common.collect.Lists.newArrayList;
import static com.keithmackay.api.domn8.nodes.elements.ImgNode.ImgConfig;
import static com.keithmackay.api.utils.UtilsKt.getLogger;

public class ImgNode extends BodyEl<ImgConfig> {

  ImgNode(ImgConfig config) {
    super(config, newArrayList());
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
          final Response response = khttp.KHttp.get(this.getSrc());
          this.src(String.format("data:%s;base64,%s",
              response.getHeaders().getOrDefault("Content-Type", "image/png"),
              Base64.getEncoder().encodeToString(response.getContent())
          ));
        } catch (Exception e) {
          getLogger(this.getClass()).error("Error Pre-Rendering Img", e);
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

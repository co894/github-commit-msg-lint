package co894;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.jackson.Jackson;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static String handleGithubEvent(Context ctx) {
    String event = ctx.getRequest().getHeaders().get("HTTP_X_GITHUB_EVENT");
    logger.info("GITHUB event type: " + event);

    if ("check_suite".equals(event)) {
      // received check suite event
      ctx.parse(Jackson.jsonNode()).then(jsonNode -> {
        String action = jsonNode.at("action").textValue();
        logger.info("GITHUB event action: " + action);
      });
    }

    return "thank you";
  }

  public static void main(String... args) throws Exception {
    RatpackServer.start(s -> s
        .serverConfig(c -> c
            .baseDir(BaseDir.find())
            .env())
        .handlers(chain -> chain
            .get("Hello!", ctx -> ctx.render("Hello!"))
            .path("web_hook", ctx -> ctx.
                byMethod(m -> m
                  .get(() -> ctx.render("Need to use POST"))
                  .post(() -> ctx.render(handleGithubEvent(ctx))))))
    );
  }
}

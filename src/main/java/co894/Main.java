package co894;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.http.Request;
import ratpack.jackson.Jackson;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static void handleGithubEvent(Context ctx) {
    Request rq = ctx.getRequest();
    logger.info("Full request:");
    logger.info(rq.getHeaders().getNettyHeaders().toString());

    rq.getBody().then(data -> {
        logger.info("Full body:\n\n" + data.getText());
        ctx.render("Thank you");
        rq.getBody().close(() -> {});
    });


//    String event = ctx.getRequest().getHeaders().get("HTTP_X_GITHUB_EVENT");
//    logger.info("GITHUB event type: " + event);
//
//    if ("check_suite".equals(event)) {
//      // received check suite event
//      ctx.parse(Jackson.jsonNode()).then(jsonNode -> {
//        String action = jsonNode.at("action").textValue();
//        logger.info("GITHUB event action: " + action);
//      });
//    }
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
                  .post(() -> handleGithubEvent(ctx)))))
    );
  }
}

package co894;

import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

public class Main {

  public static void main(final String... args) throws Exception {
    RequestHandler handler = new RequestHandler();

    RatpackServer.start(
        s ->
            s.serverConfig(c -> c.baseDir(BaseDir.find()).env())
                .handlers(
                    chain ->
                        chain
                            .get("hello", ctx -> ctx.render(handler.getHello()))
                            .get("Hello", ctx -> ctx.render(handler.getHello()))
                            .get("Hello!", ctx -> ctx.render(handler.getHello()))
                            .path(
                                "web_hook",
                                ctx ->
                                    ctx.byMethod(
                                        m ->
                                            m.get(() -> ctx.render("Need to use POST"))
                                                .post(() -> handler.handleRequest(ctx))))));
  }
}

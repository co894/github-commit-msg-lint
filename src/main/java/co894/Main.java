package co894;

import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(s -> s
        .serverConfig(c -> c
            .baseDir(BaseDir.find())
            .env())
        .handlers(chain -> chain
            .get("Hello!", ctx -> ctx.render("Hello!")))
    );
    // RepositoryService
  }
}

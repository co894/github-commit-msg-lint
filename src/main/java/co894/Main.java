package co894;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.http.Headers;
import ratpack.http.Request;
import ratpack.http.client.HttpClient;
import ratpack.jackson.Jackson;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;

import java.net.URI;
import java.util.List;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static void logRequest(Request rq) {
    Headers headers = rq.getHeaders();

    logger.info("Full request:");
    logger.info(headers.getNettyHeaders().toString());

    rq.getBody().then(data -> {
        logger.info("Full body:\n\n" + data.getText());
    });
  }

//  private static void handleGithubEvent(Context ctx) {
//    if ("check_suite".equals(event)) {
//      // received check suite event
//      ctx.parse(Jackson.jsonNode()).then(jsonNode -> {
//
//        if ("requested".equals(action) || "rerequested".equals(action)) {
//
////          String checkRunsUrl = checkSuite.get("check_runs_url").textValue();
////          String checkRunsUrl = "https://api.github.com/repos/co894/github-commit-msg-lint/check-suites/check-runs";
//          String checkRunsUrl = "https://api.github.com/repos/co894/github-commit-msg-lint/check-runs";
//
//          String repoId = repository.get("full_name").textValue();
//
//          JsonNode pullRequests = checkSuite.get("pull_requests");
//          for (JsonNode pr : pullRequests) {
//            int prId = pr.get("id").asInt();
//
//            logger.info("id: " +prId);
//            logger.info("repoId: " + repoId);
//          }
//        }
//      });
//    }
//  }

  public static void main(String... args) throws Exception {
    RequestHandler handler = new RequestHandler();

    RatpackServer.start(s -> s
        .serverConfig(c -> c
            .baseDir(BaseDir.find())
            .env())
        .handlers(chain -> chain
            .get("Hello!", ctx -> ctx.render("Hello!"))
            .path("web_hook", ctx -> ctx.
                byMethod(m -> m
                  .get(() -> ctx.render("Need to use POST"))
                  .post(() -> handler.handleRequest(ctx)))))
    );
  }
}

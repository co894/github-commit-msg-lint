package co894;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.http.Headers;
import ratpack.http.Request;
import ratpack.http.client.HttpClient;
import ratpack.jackson.Jackson;

public class RequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

  private final GitHub github;
  private final SpellChecking spellChecking;

  public RequestHandler() {
    this.github = new GitHub();
    this.spellChecking = new SpellChecking();
  }

  public void handleRequest(final Context ctx) {
    Request rq = ctx.getRequest();
    Headers headers = rq.getHeaders();

    String event = headers.get("X-Github-Event");
    LOGGER.info("GitHub event type: " + event);

    switch (event) {
      case "check_suite":
        handleCheckSuite(ctx);
        break;
      default:
        LOGGER.info("GitHub event type unknown: " + event);
    }

    ctx.render("Handling of " + event + " in progress.");
    rq.getBody().close(() -> {});
  }

  public String getHello() {
    return "Hello! It is currently " + github.formatDate(new Date());
  }

  private void handleCheckSuite(final Context ctx) {
    ctx.parse(Jackson.jsonNode())
        .then(
            jsonNode -> {
              LOGGER.info("Request body: " + jsonNode.toString());
              String action = jsonNode.get("action").textValue();
              LOGGER.info("GitHub event action: " + action);

              switch (action) {
                case "requested":
                case "rerequested":
                  createCheckRun(ctx, jsonNode);
                  break;
                default:
                  LOGGER.info("GitHub action unhandled: " + action);
              }
            });
  }

  private void createCheckRun(final Context ctx, final JsonNode requestData) {
    JsonNode checkSuite = requestData.get("check_suite");
    String headSha = checkSuite.get("head_sha").textValue();
    JsonNode prArray = checkSuite.get("pull_requests");

    if (!prArray.has(0)) {
      LOGGER.info("All PRs closed for this check suite, won't rerun");
      return;
    }

    JsonNode firstPr = prArray.get(0);
    int prNumber = firstPr.get("number").asInt();

    JsonNode repository = requestData.get("repository");
    JsonNode repoOwner = repository.get("owner");
    String owner = repoOwner.get("login").textValue();

    String repoName = repository.get("name").textValue();

    JsonNode installation = requestData.get("installation");
    int installationId = installation.get("id").asInt();

    HttpClient httpClient = ctx.get(HttpClient.class);

    Promise<Integer> checkId =
        Promise.async(
            downstream -> {
              github.indicateQueuedLinting(
                  httpClient, owner, repoName, installationId, headSha, downstream);
            });

    StringBuilder builder = new StringBuilder();

    try {
      boolean allFine = spellChecking.createSpellingReport(owner, repoName, prNumber, builder);
      github.reportLiningResults(
          httpClient, owner, repoName, installationId, allFine, builder, checkId);
    } catch (IOException e) {
      LOGGER.error("Failed spell checking PR", e);
      github.reportLiningResults(httpClient, owner, repoName, installationId, false, null, checkId);
    }
  }
}

package co894;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Consumer;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.http.client.HttpClient;
import ratpack.http.client.RequestSpec;

public class GitHub {

  private static final int JSON_WEB_TOKEN_EXPIRATION_MINUTES = 10;
  private static final String API_PREVIEW_MEDIA_TYPE =
      "application/vnd.github.antiope-preview+json";
  private static final String TOKEN_PREVIEW_MEDIA_TYPE =
      "application/vnd.github.machine-man-preview+json";

  // TODO: make configurable
  private static final String APP_NAME = "Commit Lint CO894";
  private static final int APP_ID = 17223;
  public static final String KEY_NAME = "GITHUB_PRIVATE_KEY";

  private static final MessageFormat CREATE_CHECK_RUN_URL =
      new MessageFormat("https://api.github.com/repos/{0}/{1}/check-runs");
  private static final MessageFormat UPDATE_CHECK_RUN_URL =
      new MessageFormat("https://api.github.com/repos/{0}/{1}/check-runs/{2}");

  private static final MessageFormat CREATE_ACCESS_URL =
      new MessageFormat("https://api.github.com/app/installations/{0}/access_tokens");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Logger LOGGER = LoggerFactory.getLogger(GitHub.class);

  private static final class Token {
    public final String token;
    public final Date expiration;

    Token(final String token, final Date expiration) {
      this.token = token;
      this.expiration = expiration;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Token that = (Token) o;
      return Objects.equals(token, that.token) && Objects.equals(expiration, that.expiration);
    }

    @Override
    public int hashCode() {
      return Objects.hash(token, expiration);
    }
  }

  /** App Installation Id -> JsonWebToken. */
  private final HashMap<Integer, Token> jsonWebTokens;

  /** App Installation Id -> access token. */
  private final HashMap<Integer, Token> accessTokens;

  private final SimpleDateFormat dateFormat;

  public GitHub() {
    jsonWebTokens = new HashMap<>();
    accessTokens = new HashMap<>();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("Zulu"));
  }

  public String formatDate(final Date date) {
    return dateFormat.format(date) + "Z";
  }

  public static PrivateKey readPrivateKey(final String keyName)
      throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
    String privateKeyContent = System.getenv(keyName);
    privateKeyContent = privateKeyContent.split("-----")[2];
    privateKeyContent = privateKeyContent.replaceAll("\n", "");

    byte[] encodedKey = Base64.getDecoder().decode(privateKeyContent);

    ASN1Sequence primitive = (ASN1Sequence) ASN1Sequence.fromByteArray(encodedKey);
    Enumeration<?> e = primitive.getObjects();
    BigInteger v = ((ASN1Integer) e.nextElement()).getValue();

    int version = v.intValue();
    if (version != 0 && version != 1) {
      throw new IllegalArgumentException("wrong version for RSA private key");
    }

    /* In fact only modulus and private exponent are in use. */
    BigInteger modulus = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger publicExponent = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger privateExponent = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger prime1 = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger prime2 = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger exponent1 = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger exponent2 = ((ASN1Integer) e.nextElement()).getValue();
    BigInteger coefficient = ((ASN1Integer) e.nextElement()).getValue();

    RSAPrivateKeySpec spec = new RSAPrivateKeySpec(modulus, privateExponent);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PrivateKey pk = kf.generatePrivate(spec);
    return pk;
  }

  private static Date addMinutes(final Date date, final int amount) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.MINUTE, amount);
    return cal.getTime();
  }

  private Token determineJsonWebToken(final int appId, final PrivateKey key) {
    Date now = new Date();
    Date inTenMinutes = addMinutes(now, JSON_WEB_TOKEN_EXPIRATION_MINUTES);

    String jsonWebToken =
        Jwts.builder()
            .setIssuer("" + appId)
            .setIssuedAt(new Date())
            .setExpiration(inTenMinutes)
            .signWith(key, SignatureAlgorithm.RS256)
            .compact();
    return new Token(jsonWebToken, inTenMinutes);
  }

  private String getCheckRunsUrl(final String owner, final String repoName) {
    return CREATE_CHECK_RUN_URL.format(new Object[] {owner, repoName});
  }

  private String getUpdateCheckRunUrl(
      final String owner, final String repoName, final int checkId) {
    return UPDATE_CHECK_RUN_URL.format(new Object[] {owner, repoName, "" + checkId});
  }

  private String getCreateAccessTokenUrl(final int installationId) {
    return CREATE_ACCESS_URL.format(new Object[] {"" + installationId});
  }

  private void withJsonWebToken(final int installationId, final Consumer<String> fn) {
    Token token = jsonWebTokens.get(installationId);
    Date now = new Date();
    if (token == null || token.expiration.before(addMinutes(now, 1))) {
      try {
        PrivateKey key = readPrivateKey(KEY_NAME);
        token = determineJsonWebToken(APP_ID, key);
        jsonWebTokens.put(installationId, token);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
        LOGGER.error(e.toString());
        return;
      }
    }

    fn.accept(token.token);
  }

  private void createAccessToken(
      final int installationId, final HttpClient httpClient, final Consumer<String> fn) {
    String createAccessTokenUrl = getCreateAccessTokenUrl(installationId);
    withJsonWebToken(
        installationId,
        jsonWebToken -> {
          httpClient
              .post(
                  URI.create(createAccessTokenUrl),
                  req -> {
                    req.getHeaders()
                        .add("Authorization", "Bearer " + jsonWebToken)
                        .add("Accept", TOKEN_PREVIEW_MEDIA_TYPE)
                        .add("User-Agent", APP_NAME);
                  })
              .onError(t -> LOGGER.error(t.toString()))
              .then(
                  res -> {
                    String body = res.getBody().getText();
                    LOGGER.info(
                        "Http request to "
                            + createAccessTokenUrl
                            + "\nStatus: "
                            + res.getStatusCode()
                            + "\nHeaders: "
                            + res.getHeaders().getNettyHeaders().toString()
                            + "\nFull body:\n\n"
                            + body);

                    JsonNode node = MAPPER.reader().readTree(body);
                    String accessToken = node.get("token").textValue();
                    Date expiration =
                        DatatypeConverter.parseDateTime(node.get("expires_at").textValue())
                            .getTime();

                    accessTokens.put(installationId, new Token(accessToken, expiration));
                    fn.accept(accessToken);
                  });
        });
  }

  private void withAccessToken(
      final int installationId, final HttpClient httpClient, final Consumer<String> fn) {
    Token token = accessTokens.get(installationId);
    Date now = new Date();
    if (token == null || token.expiration.before(addMinutes(now, 1))) {
      createAccessToken(installationId, httpClient, fn);
    } else {
      fn.accept(token.token);
    }
  }

  public void indicateQueuedLinting(
      final HttpClient httpClient,
      final String owner,
      final String repoName,
      final int installationId,
      final String headSha,
      final Downstream<? super Integer> checkIdResolver) {
    String checkRunsUrl = getCheckRunsUrl(owner, repoName);
    withAccessToken(
        installationId,
        httpClient,
        accessToken -> {
          httpClient
              .post(
                  URI.create(checkRunsUrl),
                  req -> {
                    addAuthorizationHeaders(req, accessToken);

                    ObjectNode requestNode =
                        MAPPER
                            .createObjectNode()
                            .put("name", "commit-lint")
                            .put("head_sha", headSha)
                            .put("status", "queued");

                    String requestJson = requestNode.toString();

                    LOGGER.info("Sending POST to " + checkRunsUrl + "\nBody:\n" + requestJson);

                    req.getBody().text(requestJson);
                  })
              .onError(
                  t -> {
                    LOGGER.error("HTTP Request to create check run failed.", t);
                    checkIdResolver.error(t);
                  })
              .then(
                  res -> {
                    String body = res.getBody().getText();

                    JsonNode node = MAPPER.reader().readTree(body);
                    int checkId = node.get("id").asInt();
                    checkIdResolver.success(checkId);

                    LOGGER.info(
                        "Http request to "
                            + checkRunsUrl
                            + "\nStatus: "
                            + res.getStatusCode()
                            + "\nHeaders: "
                            + res.getHeaders().getNettyHeaders().toString()
                            + "\nFull body:\n\n"
                            + body);
                  });
        });
  }

  private void addAuthorizationHeaders(final RequestSpec req, final String accessToken) {
    req.getHeaders()
        .add("Accept", API_PREVIEW_MEDIA_TYPE)
        .add("Authorization", "token " + accessToken)
        .add("Content-Type", "application/json")
        .add("User-Agent", APP_NAME);
  }

  public void reportLiningResults(
      final HttpClient httpClient,
      final String owner,
      final String repoName,
      final int installationId,
      final boolean allFine,
      final StringBuilder builder,
      final Promise<Integer> checkId) {
    withAccessToken(
        installationId,
        httpClient,
        accessToken -> {
          checkId.then(
              (final Integer id) -> {
                String url = getUpdateCheckRunUrl(owner, repoName, id);
                httpClient
                    .request(
                        URI.create(url),
                        action -> {
                          action.patch();
                          addAuthorizationHeaders(action, accessToken);

                          Date completion = new Date();
                          String conclusion = allFine ? "success" : "neutral";

                          ObjectNode requestNode =
                              MAPPER
                                  .createObjectNode()
                                  .put("name", "commit-lint")
                                  .put("status", "completed")
                                  .put("completed_at", formatDate(completion))
                                  .put("conclusion", conclusion);

                          if (!allFine) {
                            ObjectNode outputNode =
                                MAPPER
                                    .createObjectNode()
                                    .put("title", "Found issues in commit messages")
                                    .put("summary", "Found spelling issues")
                                    .put("text", builder.toString());

                            requestNode.set("output", outputNode);
                          }

                          String requestJson = requestNode.toString();
                          LOGGER.info("Sending PATCH to " + url + "\nBody:\n" + requestJson);
                          action.getBody().text(requestJson);
                        })
                    .onError(t -> LOGGER.error("HTTP Request to update check run failed.", t))
                    .then(
                        res -> {
                          String body = res.getBody().getText();

                          JsonNode node = MAPPER.reader().readTree(body);

                          LOGGER.info(
                              "Http request to "
                                  + url
                                  + "\nStatus: "
                                  + res.getStatusCode()
                                  + "\nHeaders: "
                                  + res.getHeaders().getNettyHeaders().toString()
                                  + "\nFull body:\n\n"
                                  + body);
                        });
              });
        });
  }
}

package co894;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.http.MutableHeaders;
import ratpack.http.client.HttpClient;
import ratpack.jackson.Jackson;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class GitHub {

  private static final int JSON_WEB_TOKEN_EXPIRATION_MINUTES = 10;
  private static final String API_PREVIEW_MEDIA_TYPE = "application/vnd.github.antiope-preview+json";
  private static final String TOKEN_PREVIEW_MEDIA_TYPE = "application/vnd.github.machine-man-preview+json";

  // TODO: make configurable
  private static final String APP_NAME = "Commit Lint CO894";
  private static final int APP_ID = 17223;
  public static final String KEY_NAME = "commit-lint-co894.2018-09-08.private-key.pem";

  private static final MessageFormat CHECK_RUNS_URL = new MessageFormat("https://api.github.com/repos/{0}/{1}/check-runs");

  private static final MessageFormat CREATE_ACCESS_URL =
      new MessageFormat("https://api.github.com/app/installations/{0}/access_tokens");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Logger LOGGER = LoggerFactory.getLogger(GitHub.class);

  private static final class Token {
    public final String token;
    public final Date expiration;

    Token(String token, Date expiration) {
      this.token = token;
      this.expiration = expiration;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Token that = (Token) o;
      return Objects.equals(token, that.token) &&
          Objects.equals(expiration, that.expiration);
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



  public GitHub() {
    jsonWebTokens = new HashMap<>();
    accessTokens = new HashMap<>();
  }

  public static PrivateKey readPrivateKey(String keyName) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
    File file = new File(keyName);
    String privateKeyContent = new String(Files.readAllBytes(Paths.get(file.toURI())));
    privateKeyContent = privateKeyContent.split("-----")[2];
    privateKeyContent = privateKeyContent.replaceAll("\n", "");

    byte[] encodedKey = Base64.getDecoder().decode(privateKeyContent);

    ASN1Sequence primitive = (ASN1Sequence) ASN1Sequence
        .fromByteArray(encodedKey);
    Enumeration<?> e = primitive.getObjects();
    BigInteger v = ((ASN1Integer) e.nextElement()).getValue();

    int version = v.intValue();
    if (version != 0 && version != 1) {
      throw new IllegalArgumentException("wrong version for RSA private key");
    }
    /**
     * In fact only modulus and private exponent are in use.
     */
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

  private static Date addMinutes(Date date, int amount) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.MINUTE, amount);
    return cal.getTime();
  }

  private Token determineJsonWebToken(int appId, PrivateKey key) {
    Date now = new Date();
    Date inTenMinutes = addMinutes(now, JSON_WEB_TOKEN_EXPIRATION_MINUTES);

    String jsonWebToken = Jwts.builder()
        .setIssuer("" + appId)
        .setIssuedAt(new Date())
        .setExpiration(inTenMinutes)
        .signWith(key, SignatureAlgorithm.RS256)
        .compact();
    return new Token(jsonWebToken, inTenMinutes);
  }

  private String getCheckRunsUrl(String owner, String repoName) {
    return CHECK_RUNS_URL.format(new Object[] {owner, repoName});
  }

  private String getCreateAccessTokenUrl(int installationId) {
    return CREATE_ACCESS_URL.format(new Object[] {"" + installationId});
  }

  private void withJsonWebToken(int installationId, Consumer<String> fn) {
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

  private void createAccessToken(int installationId, HttpClient httpClient, Consumer<String> fn) {
    String createAccessTokenUrl = getCreateAccessTokenUrl(installationId);
    withJsonWebToken(installationId, jsonWebToken -> {
      httpClient.post(URI.create(createAccessTokenUrl), req -> {
        req.getHeaders()
            .add("Authorization", "Bearer " + jsonWebToken)
            .add("Accept", TOKEN_PREVIEW_MEDIA_TYPE)
            .add("User-Agent", APP_NAME);
      }).onError(t -> LOGGER.error(t.toString()))
          .then(res -> {
            String body = res.getBody().getText();
            LOGGER.info("Http request to " + createAccessTokenUrl + "\nStatus: " + res.getStatusCode() +
                "\nHeaders: " + res.getHeaders().getNettyHeaders().toString() +
                "\nFull body:\n\n" + body);

            JsonNode node = MAPPER.reader().readTree(body);
            String accessToken = node.get("token").textValue();
            Date expiration = DatatypeConverter.parseDateTime(node.get("expires_at").textValue()).getTime();

            accessTokens.put(installationId, new Token(accessToken, expiration));
            fn.accept(accessToken);
          });
    });
  }

  private void withAccessToken(int installationId, HttpClient httpClient, Consumer<String> fn) {
    Token token = accessTokens.get(installationId);
    Date now = new Date();
    if (token == null || token.expiration.before(addMinutes(now, 1))) {
      createAccessToken(installationId, httpClient, fn);
    } else {
      fn.accept(token.token);
    }
  }

  public void indicateQueuedLinting(HttpClient httpClient, String owner,
                                    String repoName, int installationId /*326373*/,
                                    String headSha) {
    String checkRunsUrl = getCheckRunsUrl(owner, repoName);
    withAccessToken(installationId, httpClient, accessToken -> {
      httpClient.post(URI.create(checkRunsUrl), req -> {
        req.getHeaders()
            .add("Accept", API_PREVIEW_MEDIA_TYPE)
            .add("Authorization", "token " + accessToken)
            .add("Content-Type", "application/json")
            .add("User-Agent", APP_NAME);

        ObjectNode requestNode = MAPPER.createObjectNode()
            .put("name", "commit-lint")
            .put("head_sha", headSha)
            .put("status", "queued");

        String requestJson = requestNode.toString();

        LOGGER.info("Sending POST to " + checkRunsUrl + "\nBody:\n" + requestJson);

        req.getBody().text(requestJson);
      })
          .onError(t -> LOGGER.error(t.toString()))
          .then(res -> {
            LOGGER.info("Http request to " + checkRunsUrl + "\nStatus: " + res.getStatusCode() +
                "\nHeaders: " + res.getHeaders().getNettyHeaders().toString() +
                "\nFull body:\n\n" + res.getBody().getText());
          });
    });
  }
}

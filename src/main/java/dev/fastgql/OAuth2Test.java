package dev.fastgql;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

import java.util.Map;

public class OAuth2Test extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand(
      "run", OAuth2Test.class.getName());
  }

  private static final String CLIENT_ID = "7700a3772ef4b963ee62";
  private static final String CLIENT_SECRET = "b572c9b9f50fb2730696fcc4120c4e2cc6fcd3dc";

  @Override
  public void start() {

    final JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setPublicKey("secret")
        .setSymmetric(true)));

    final Router router = Router.router(vertx);
    final OAuth2Auth authProvider =
      GithubAuth.create(vertx, CLIENT_ID, CLIENT_SECRET);

    router.route("/jwt").handler(
      OAuth2AuthHandler.create(authProvider)
        .setupCallback(router.route("/callback"))
    );

    router.get("/jwt").handler(ctx -> {
      AccessToken user = (AccessToken) ctx.user();
      user.userInfo(res -> {
        if (res.failed()) {
          ctx.fail(res.cause());
        } else {
          final JsonObject userInfo = res.result();
          final String jwtToken = jwtAuth.generateToken(new JsonObject(Map.of("user", userInfo.getString("login"))));
          ctx.response()
            .putHeader(HttpHeaders.LOCATION, String.format("/jwtcallback?jwt=%s", jwtToken))
            .setStatusCode(302)
            .end("redirecting");
        }
      });
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }
}

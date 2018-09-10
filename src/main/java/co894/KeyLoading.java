package co894;

import java.security.PrivateKey;

public class KeyLoading {
  public static void main(String... args) throws Exception {
    GitHub gh = new GitHub();
    PrivateKey key = gh.readPrivateKey(GitHub.KEY_NAME);

    System.out.println(key);
  }
}

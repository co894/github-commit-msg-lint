package co894;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.PrivateKey;
import org.junit.jupiter.api.Test;

public class PrivateKeyTest {
  /** Generated with: openssl genrsa -f4 -out private.txt 4096 */
  private static final String TEST_KEY =
      "-----BEGIN RSA PRIVATE KEY-----\n"
          + "MIIJKAIBAAKCAgEAv8J3F6nGl+iRFeeRV74vCpgxKZslPgMqlIKYuGwO39zrpabt\n"
          + "306bh3hbjVobZWYpgrFkHmro//bgjFn6uICwdeqa9BApJCNm1Rpri+d1wKkXdCmF\n"
          + "h4Ca1joAydIbR/N9W7c2f8IYSrfrVTlNDimQkIrStXb8a8UuD8ZZTbsKrBwTry30\n"
          + "rRN841dKFVkK+rZqZ+LTrouVl7kqOYhijF+epvXS787be6BgTvx3wDY8Z16ux+sj\n"
          + "ERIWLK18AyxgJS8TffeGD3u8vuwKeoSZOJVUcftXGUkJdHCTKpO/tAGqDddwjl2W\n"
          + "ZMUxu0ff3y2ny1C7S6BSQySH0NZXIbuR2LhKxm71WFr3ntjip08Y4QdCZBKfM+Eg\n"
          + "nYmR+0/Qm2d0t29eC99WXrXN+M61n6an77dU+wwPkzBYr3LlemshRxcQnfUkcy6l\n"
          + "GZIUGfddmYi4O1qVqZJJSoxoRDP7FXKJRi6X4HxFxEY2SrYfg9RoADi4Pt3u6R8p\n"
          + "BSLVIziKM2EG/DXjf7pS+e/kysHri91HY+6jADWbn7YlTtImx3zFvFEoalBCBSSE\n"
          + "V8VgSxOzscvULJTyIdW85KJxHw+QEAivPPTBjWPodDvL5GbLCF3S1byexntwU+V1\n"
          + "Jxio9xcvq5Y/ncpUQDDo5w4nFJCjDJLoY5e4geWlkYEj6LX6ZBJuv+ViW/8CAwEA\n"
          + "AQKCAgEAteeqg6cI/eefYhEWng6Kn1k6IcbL8GSeitzMNP5EqfXvEGgm9JOkFEGH\n"
          + "T7KvlGPycskGOZifSNkPr/RXU5i4TFNuBKIj9OUkUnlKlh/OYyHEXuxYf+7uctP5\n"
          + "FXqbaIjBXTSSnWUpGBAaTPEbeJjTS1p1gbDKyQz2of49jvAkspm7zo0gsuJusVaH\n"
          + "r3KVaLmMfNn+hnBhStlbBdKr8Y9vd+Bv5g7rw/2h/queiZapfEGkOX+D91iZ8m3r\n"
          + "2zLITd23duJMqPlvVgmh8xFALB30Sl/sAs93CZ8U6+nWi9KcV58MZfC/jv5FK+yF\n"
          + "oTe1GbiOw7Hk9057DA+X/ibptpU0UBr2N/DuCfdYN5t61ElpEpKiVDbpj6jcE4dR\n"
          + "bst35tR6v8ZKSE3xfoalsB2yENrMcFy8U4wOYb/l0xUqs6pDux0btVH8jn0yIg7B\n"
          + "mFNc5Y/tMY0riR9NuKjytMEwtEGs2RJ3i72QmiwR09d+zg47NtkhWK9kB0jaKhA6\n"
          + "QwS06VXnSSBZrXeaBX0QGiJJNBOt/0N4shwL3lnqnin9zxA8WUxS+QZV/t3+Eweg\n"
          + "DNb+tiwDZyQ2SpMZKYxxPulUQa7qpdmvVDOdYRY6GAkV4LwsOy6xn9aOHvV2VNyg\n"
          + "5da3jcKFwQmSnlcd7wZpagPQSbnXWRe7z4CaTGvRLb+TJpxaKIkCggEBAPvdEIon\n"
          + "geZwWeiXstnhGUioS4Xiy6IOBGLxigBkBheAn3fHTJV2oPKgLL99eaU5bXzZtuAg\n"
          + "58tdeLtsPIeBv2v4lrhwZmXFhs+gV9a6HGanuHC107GPTJSBenNt6yM1UWw1zxOX\n"
          + "r7ehbVjpzdBbPv4ZG5bnlTEKoW34wOECB173Dh9pHwyhKpaKJDidrwA6UMscGbIn\n"
          + "UWjSv5OSzyNfXmn9ERAOnt95ojBJhGMNMQJDPcnJvCysnuh/9iK6Nu6OoVgvSwVW\n"
          + "G6WqUDQxSW1uAUZUABH73SflVSA/mFBFtscD7f8wXLY4/Ny8YeJikFj5MzThswxQ\n"
          + "CofPFRr37uytg7MCggEBAMLosxzfgHK8bV00bfT+UQuKfZF8fZlHOaxew82L3QcI\n"
          + "urmeSaumzXfDzlh3cxkl2uUu6rQZiAIoYgfezxIpNRBhWpZbs6oDR5gKpGpw0qA6\n"
          + "UBxrkM1FkgvVaLoLkwlv1OIMIrg6iRqEsXJPy5C1XPwcgaJG+AWmPedK6pvB2B1O\n"
          + "hpUurC6zTNyauVOaPBL4e8GU6s/2t1kK3Yg1TFEGGmdcjArPzoQ/WEiSjw3/eFkK\n"
          + "K2wXM7yyqeimbsF0yFfFdZZit2BMes1DIouinpRwmEt6uM1eA7RqoQi9dQ996cm8\n"
          + "h9gC/7wrMSlmoDmqQ/tfFS0+I1h4FgJVJ8lnGhsXUIUCggEACBQBEc1Ol0uHNrSW\n"
          + "4fIcU35CXdr2WQ5GCb5hhL2wjFRtyPO1ex3A78c/aCzoF+qNnnGFMg8cICOxWaGR\n"
          + "F2+L6jSg8O15+02ok0Q7TQHPI2PBwKu0TH+iHyk/+i/OWOS+5c+cAq7czfD5ht6I\n"
          + "4WPr006O2fo3iMkkgFh4Jz9faSZXZ56BhnAs44MuwjocgM9eBAW0tUgZjlkkF+Gf\n"
          + "jyzh5FhfkbpGUjiegRn6iwrDRaVOeAhmZrBqsF5aUKbUWDZu6lsp9tPaMygRRntG\n"
          + "NaJPnCfZkLoZT8xFPLGNexWhVSTFH2g3sQiG47WPFim/I3tqrwmGKqDulYiIzsKh\n"
          + "ASpXjQKCAQA301Sx+WYd9tBQONshp9Hov1L137VNuLPRJhdpAR1ejWaGEctCCfWh\n"
          + "zwiEGDp0Qn5u1EBPM1zcWVqyrGOc7QaQ2YGLvVo5YNUZvDqZbhU15V3atgh0cu/A\n"
          + "yfBEEVqdAlX0JMuw/rsUSFtWxXolyXyHjVan9zHE1He4jqGh2SSgSESN6B9TBPIH\n"
          + "4Z0hT/TS7i7sJE4ZIK4lJ9KJ19IzBs97OWu3EQwynr4Xkzq8JKcAwcPsIr0j1/yI\n"
          + "cVsZt+xGD319hGrHf0BRGGCFpB9Nnu5P+CeEUuA2xa+4BCPKOyNCL+isuzcqWyiq\n"
          + "lgVgXt832WUEX17aURLkuiU6L3ivt77NAoIBAFCiW98Bi5YN2A9rFWw07xSlMrM9\n"
          + "63bPn3fr2SHOCYG4fZAEx9NJ1WFn/aaAgBDE5hSrH0thcxbe4soaMGLL15W1WVPN\n"
          + "LGqu4XCNbTaHHRy0AYjlXds6M/BlQCBiJN2WE/SH7UY14qa+vCkHAhDKs8JTJs3E\n"
          + "Iowrp5xDmtEzE1ZhYI6RW2uWmDYKWjp0tFj9W1blfP9IxAqiHx/RtYF7wUBXgq/g\n"
          + "jUz/t1Apjybkjuk/Hf4yY49B3fHvxuhyrx3Sjit13w0WJ1xIgiemnSFVnv0YWyZ9\n"
          + "t35bPNy9ZG1AqoVbcdcQNvNVolbUbLXhKQD9q3EM4DW6a2REkuzYDlgIj9c=\n"
          + "-----END RSA PRIVATE KEY-----\n";

  @Test
  void readKey() throws Exception {
    PrivateKey privKey = GitHub.readPrivateKey(TEST_KEY);

    assertEquals("RSA", privKey.getAlgorithm());
    assertEquals("PKCS#8", privKey.getFormat());
  }
}

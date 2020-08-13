package dev.fastgql.integration;

import java.util.Map;

public interface WithSecurity extends WithFastGQL {

  @Override
  default boolean isSecurityActive() {
    return true;
  }

  @Override
  default Map<String, Object> createSecurityConfigEntry() {
    return Map.of(
        "algorithm", "RS256",
        "publicKey",
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvAOEBHuP9Pie9si3Ok3e/rBOYhyJC6jR+P6Np38biqxUbZRfvmXpS4sdsUIIiMP14C5sfzDtT+oe+FEOyleCjRY2aXCHUmqQHyxmUMe4sBvxn5wLoCDi528dpeDsq3QVvL+X0zgn266ZgCP2xwoXtc0ry8Y397jTonC90/jnawizn4rqm7s14ChPM1CUn5rzfGsd1/8BvyR3H7F2wGcgQlNYpKW8ge1YsvtmIzZswc6u2JWUg6wh1Is1BweBDULvJVONyCzyt97CyYp2Si5ISODrfe+VzYZnPfJ+mtuUQxa3+YivK1Hz0rFotK1cmDCpJ8AqfMKDnyh/urYWqAXFVQIDAQAB");
  }
}
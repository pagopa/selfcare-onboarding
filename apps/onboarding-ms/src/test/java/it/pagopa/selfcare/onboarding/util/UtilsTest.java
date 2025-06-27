package it.pagopa.selfcare.onboarding.util;


import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class UtilsTest {

  @Test
  void getFileExtension() {
    final var filename = "test.pdf";
    var extension = Utils.getFileExtension(filename);
    assertEquals("pdf", extension);
  }

  @Test
  void getFileExtensionWithTwoParts() {
    final var filename = "index/test.pdf";
    var extension = Utils.getFileExtension(filename);
    assertEquals("pdf", extension);
  }

  @Test
  void getFileExtensionWithMoreThanTwoParts() {
    final var filename = "index/test/test.pdf";
    var extension = Utils.getFileExtension(filename);
    assertEquals("pdf", extension);
  }

}

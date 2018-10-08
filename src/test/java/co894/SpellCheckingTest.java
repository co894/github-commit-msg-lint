package co894;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

public class SpellCheckingTest {

  @Test
  void checkSpellingOfString() throws IOException {
    JLanguageTool tool = new JLanguageTool(new AmericanEnglish());

    List<RuleMatch> matches = tool.check("Check");
    assertEquals(0, matches.size());
  }

  @Test
  void checkSimpleTypo() throws IOException {
    JLanguageTool tool = new JLanguageTool(new AmericanEnglish());

    List<RuleMatch> matches = tool.check("Colour");
    assertEquals(1, matches.size());

    RuleMatch m = matches.get(0);
    assertEquals("Spelling mistake", m.getShortMessage());
  }
}

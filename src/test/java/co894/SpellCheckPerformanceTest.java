package co894;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

public class SpellCheckPerformanceTest {
  private static String[] COMMIT_MSGS =
      new String[] {
        "Merge branch 'demo'\n" + "\n",
        "Remove unused code, replaced by tests.\n\n",
        "Test our repo\n",
        "Merge PR #6: Update build.gradle\n",
        "Merge PR #7: Added basic tests and CI setup\n",
        "Added basic tests and CI setup\n"
            + "\n"
            + "Signed-off-by: Stefan Marr <git@stefan-marr.de>\n",
        "Merge PR #5: Format with Eclipse and add Date to Hello World\n",
        "Add date to hello world url\n"
            + "\n"
            + "Signed-off-by: Stefan Marr <git@stefan-marr.de>\n",
        "Update build.gradle\n",
        "update authors again\n" + "Another attmpt.",
        "Format with Eclipse\n"
            + "\n"
            + "- this orders imports differently than before\n"
            + "- it applies some other code changes that are nice to have for larger projects\n"
            + "\n"
            + "Signed-off-by: Stefan Marr <git@stefan-marr.de>\n",
        "Merge PR #3: Add a check to avoid a null pointer exception when PR is closed\n" + "\n\n"
      };

  @Test
  void runSpellCheck1() throws IOException {
    List<RuleMatch> matches = getSpellingIssues(COMMIT_MSGS[0]);
    assertTrue(matches.size() >= 0);
  }

  @Test
  void runSpellCheckAll() throws IOException {
    for (int i = 0; i < 20; i++)
      for (String msg : COMMIT_MSGS) {

        List<RuleMatch> matches = getSpellingIssues(msg);
        assertTrue(matches.size() >= 0);
      }
  }

  private List<RuleMatch> getSpellingIssues(final String msg) throws IOException {
    Language lang = new AmericanEnglish();
    JLanguageTool tool = new JLanguageTool(lang);

    List<RuleMatch> matches = tool.check(msg);
    return matches;
  }
}

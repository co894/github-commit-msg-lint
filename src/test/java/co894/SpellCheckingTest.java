package co894;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.junit.jupiter.api.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  @Test
  void spellCheckingIssueWithNames() throws IOException {
    String msg =
        "Remove SpellChecking.main(), replaced by tests\n"
            + "\n"
            + "Signed-off-by: Stefan Marr <git@stefan-marr.de>";
    JLanguageTool tool = new JLanguageTool(new AmericanEnglish());

    List<RuleMatch> matches = tool.check(msg);

    assertEquals(0, matches.size());
  }

  // ..

  // ..

  // ..

  // ..

  // ..

  // ..

  //  @Test
  void spellCheckingIssueWithNames2() throws IOException {
    Logger LOGGER = LoggerFactory.getLogger(SpellChecking.class);

    String msg =
        "Remove SpellChecking.main(), replaced by tests\n"
            + "\n"
            + "Signed-off-by: Stefan Marr <git@stefan-marr.de>";
    JLanguageTool tool = new JLanguageTool(new AmericanEnglish());

    List<RuleMatch> matches = tool.check(msg);

    System.out.println(matches.toString());

    LOGGER.debug(matches.toString());
    LOGGER.trace(matches.toString());
    LOGGER.info(matches.toString());
    LOGGER.warn(matches.toString());
    LOGGER.error(matches.toString());

    assertEquals(0, matches.size());
  }

  // ..

  // ..

  // ..

  // ..

  // ..

  // ..

  @Test
  void spellCheckingIssueWithNamesTemplate() throws IOException {
    PullRequestService prService = new PullRequestService();
    List<RepositoryCommit> commits =
        prService.getCommits(RepositoryId.create("co894", "github-commit-msg-lint"), 9);

    assertEquals(1, commits.size());

    doSpellCheck(commits);
  }

  private void doSpellCheck(final List<RepositoryCommit> commits) throws IOException {
    // get commit message from github
    String msg = null;

    // and get name
    String[] nameParts = null;

    JLanguageTool tool = new JLanguageTool(new AmericanEnglish());
    List<RuleMatch> matches = tool.check(msg);

    // ignore the name from misspelling

    //      String m = getMatchedStringInMessage(match, msg);

    //      for (String name : nameParts) {
    //        if (name.equals(m)) {
    //          matches.remove(match);
    //          break;
    //        }
    //      }

    assertEquals(0, matches.size());
  }

  private String getMatchedStringInMessage(final RuleMatch match, final String msg) {
    return msg.substring(match.getFromPos(), match.getToPos());
  }

  // ..

  // ..

  // ..

  // ..

  // ..

  // ..

  //  @Test
  void spellCheckingIssueWithNames3() throws IOException {
    PullRequestService prService = new PullRequestService();
    List<RepositoryCommit> commits =
        prService.getCommits(RepositoryId.create("co894", "github-commit-msg-lint"), 9);

    assertEquals(1, commits.size());
    RepositoryCommit commit = commits.get(0);

    String msg = commit.getCommit().getMessage();

    // String[] nameParts = commit.getAuthor().getName().split(" ");

    String[] nameParts = commit.getCommit().getAuthor().getName().split(" ");

    Language lang = new AmericanEnglish();
    JLanguageTool tool = new JLanguageTool(lang);

    List<RuleMatch> matches = tool.check(msg);
    for (RuleMatch match : new ArrayList<>(matches)) {
      // for (RuleMatch match : matches) {
      String m = getMatchedStringInMessage(match, msg);

      for (String name : nameParts) {
        if (name.equals(m)) {
          matches.remove(match);
          break;
        }
      }
    }

    assertEquals(0, matches.size());
  }
}

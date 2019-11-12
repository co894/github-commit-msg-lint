package co894;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.junit.jupiter.api.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import edu.berkeley.nlp.lm.util.Logger;

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
		String msg = "Remove SpellChecking.main(), replaced by tests\n" + "\n"
				+ "Signed-off-by: Stefan Marr <git@stefan-marr.de>";
		JLanguageTool tool = new JLanguageTool(new AmericanEnglish());

		List<RuleMatch> matches = tool.check(msg);
		System.out.println(matches.toString());
		assertEquals(0, matches.size());
	}

	private String getCommitMessages(List<RepositoryCommit> commits) {
		return null;
	}
	
	private String[] getNameParts(List<RepositoryCommit> commits) {
		return null;
	}
	
	@Test
	void spellCheckingIssueWithNamesTemplate() throws IOException {
		PullRequestService prService = new PullRequestService();
		List<RepositoryCommit> commits = prService.getCommits(RepositoryId.create("co894", "github-commit-msg-lint"),
				9);

		assertEquals(1, commits.size());

		// TODO

		// get commit message from github
		String msg = getCommitMessages(commits);

		// and get name
		String[] nameParts = getNameParts(commits);

		JLanguageTool tool = new JLanguageTool(new AmericanEnglish());
		List<RuleMatch> matches = tool.check(msg);

		// ignore the name from misspelling

		// getMatchedStringInMessage(match, msg);

		assertEquals(0, matches.size());
	}

}

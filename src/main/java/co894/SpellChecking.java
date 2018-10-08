package co894;

import java.io.IOException;
import java.util.List;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

public class SpellChecking {

  private final JLanguageTool tool;
  private final PullRequestService prService;

  public SpellChecking() {
    this.tool = new JLanguageTool(new AmericanEnglish());
    this.prService = new PullRequestService();
  }

  public boolean createSpellingReport(
      final String owner, final String repo, final int prNumber, final StringBuilder reportBuilder)
      throws IOException {
    boolean allFine = true;

    List<RepositoryCommit> commits =
        prService.getCommits(RepositoryId.create(owner, repo), prNumber);
    for (RepositoryCommit commit : commits) {
      boolean isFine = checkSpelling(commit, tool, reportBuilder, owner + "/" + repo, prNumber);
      allFine = allFine && isFine;
    }

    return allFine;
  }

  private static boolean checkSpelling(
      final RepositoryCommit repoCommit,
      final JLanguageTool tool,
      final StringBuilder builder,
      final String repoId,
      final int prNumber)
      throws IOException {
    String msg = repoCommit.getCommit().getMessage();
    List<RuleMatch> matches = tool.check(msg);

    if (matches.isEmpty()) {
      return true;
    } else {
      builder.append("**Commit:** ");
      builder.append("[");
      builder.append(repoCommit.getSha());
      builder.append("](https://github.com/");
      builder.append(repoId);
      builder.append("/pull/");
      builder.append(prNumber);
      builder.append("/commits/");
      builder.append(repoCommit.getSha());
      builder.append(")\n\n");

      builder.append("```\n");
      builder.append(msg);
      builder.append("\n```\n\n\n");

      builder.append("Found ");
      builder.append(matches.size());
      builder.append(" potential issues in commit messages.\n\n");

      for (RuleMatch match : matches) {
        builder.append("- ");

        String problematicText = msg.substring(match.getFromPos(), match.getToPos());
        builder.append('*');
        builder.append(problematicText);
        builder.append('*');
        builder.append("  \n");
        builder.append("    ");
        builder.append(match.getMessage());
        builder.append("  \n");

        builder.append("    Suggested corrections: ");
        builder.append(match.getSuggestedReplacements());
        builder.append("  \n");
        builder.append("    char idx: ");
        builder.append(match.getFromPos());
        builder.append("\n\n\n");
      }
    }

    return false;
  }
}

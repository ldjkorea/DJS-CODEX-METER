package dev.bennett.codexmeter;

/** Canonical production repository used for release discovery and project links. */
public final class GitHubReleaseSource {
    public static final String REPOSITORY_URL =
            "https://github.com/ldjkorea/DJS-GPT-HUD"; // pragma: allowlist secret
    public static final String RELEASES_API_URL =
            "https://api.github.com/repos/ldjkorea/DJS-GPT-HUD/releases?per_page=30"; // pragma: allowlist secret

    private GitHubReleaseSource() {
    }
}

package com.jetbrains.internship.castcommoncommitsfinder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GithubLastCommonCommitsFinderTest {

    // Github limits amount of calls per hour to 60
    // if you are unauthorised. thus, change this flag
    // to use the token in all tests
    // This token is for my machine user profile,
    // so there is no sensitive info :)
    private final boolean useToken = false;
    private final String token = "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";


    /**
     * This test connects to my repo and finds last common commits of my
     * main branch and a branch that points to the initial commit.
     * The last common commit of those two must always be the initial commit.
     */
    @Test
    public void mainWithInitialCommitTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches",
                        useToken ? token : null);

        String initialCommitBranch = "test-initial-commit";
        String mainBranch = "main";

        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(mainBranch, initialCommitBranch);
            assertEquals(1, lastCommonCommits.size());
            assertEquals("23ca9bc4dec619be229ab276055f233c7b1f0dce", lastCommonCommits.iterator().next());
        } catch (IOException e) {
            fail();
        }

    }

    /**
     * This test checks the following simplest case:
     *          C
     *        /
     * A -> B
     *        \
     *          D
     * Here we test for branches C and D, and B should be the answer
     */
    @Test
    public void simpleTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches",
                        useToken ? token : null);

        String simpleBranchA = "simple-branch-test-A";
        String simpleBranchB = "simple-branch-test-B";

        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(simpleBranchA, simpleBranchB);
            assertEquals(1, lastCommonCommits.size());
            assertEquals("ae7f4ca270fa224df83370d0806958914918bc70", lastCommonCommits.iterator().next());
        } catch (IOException e) {
            fail();
        }

    }

    /**
     * This test demonstrates a case where there are multiple last common commits.
     * The case can be drawn as such:
     *       C - D - G
     *      /     \ /
     * A - B       /
     *      \     / \
     *       E - F - H
     *
     * Here G and H have two last common commits: D and F.
     */
    @Test
    public void multipleLastCommitsTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches",
                        useToken ? token : null);

        List<String> expectedAnswer = Arrays.asList(
                "a9637bf9aad9a75e0afa9d5e685a02e880d221b4",
                "91ca5b9c041479b9a046788691e35cd2f0743dca");

        String multipleLastCommonCommitsBranchA = "multiple-last-common-commits-branch-A";
        String multipleLastCommonCommitsBranchB = "multiple-last-common-commits-branch-B";

        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(multipleLastCommonCommitsBranchA,
                            multipleLastCommonCommitsBranchB);
            assertEquals(2, lastCommonCommits.size());
            assertTrue(expectedAnswer.containsAll(lastCommonCommits));
            assertTrue(lastCommonCommits.containsAll(expectedAnswer));
        } catch (IOException e) {
            fail();
        }

    }

    /**
     * This test shows token usage. It uses my machine user, which has
     * a little repository.
     */
    @Test
    public void tokenTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111-machine-user", "jetbrains-token-test",
                        token);

        String simpleBranchA = "main";
        String simpleBranchB = "dev";

        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(simpleBranchA, simpleBranchB);
            assertEquals(1, lastCommonCommits.size());
            assertEquals("d27c0bb10d185b0eeb63060e752f920b711f95a7", lastCommonCommits.iterator().next());
        } catch (IOException e) {
            fail();
        }

    }

    @Test
    public void nonExistentRepo() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "thisPublicRepoDoesntExist",
                        null);

        String simpleBranchA = "main";
        String simpleBranchB = "dev";

        try {
            lastCommonCommitsFinder.findLastCommonCommits(simpleBranchA, simpleBranchB);
            fail();
        } catch (IOException ignored) {
        }

    }

}

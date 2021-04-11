package com.jetbrains.internship.castcommoncommitsfinder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class GithubLastCommonCommitsFinderTest {

    private final String initialCommitBranch = "test-initial-commit";

    private final String mainBranch = "main";

    private final String simpleBranchA = "simple-branch-test-A";
    private final String simpleBranchB = "simple-branch-test-B";

    private final String multipleLastCommonCommitsBranchA = "multiple-last-common-commits-branch-A";
    private final String multipleLastCommonCommitsBranchB = "multiple-last-common-commits-branch-B";

    @Test
    public void mainWithMainTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches", null);


        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(mainBranch, mainBranch);
            lastCommonCommits.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void mainWithInitialCommitTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches", null);


        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(mainBranch, initialCommitBranch);
            lastCommonCommits.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void simpleTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches",
                        "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");


        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(simpleBranchA, simpleBranchB);
            lastCommonCommits.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void multipleLastCommitsTest() {

        LastCommonCommitsFinder lastCommonCommitsFinder = new GithubCommonCommitsFinderFactory()
                .create("ghB111", "jetbrains-preliminary-merge-builds-of-pull-request-branches",
                        "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");


        try {
            Collection<String> lastCommonCommits =
                    lastCommonCommitsFinder.findLastCommonCommits(multipleLastCommonCommitsBranchA,
                            multipleLastCommonCommitsBranchB);
            lastCommonCommits.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
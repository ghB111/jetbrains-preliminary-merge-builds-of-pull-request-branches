package com.jetbrains.internship.castcommoncommitsfinder;

public class GithubCommonCommitsFinderFactory implements LastCommonCommitsFinderFactory {

    @Override
    public LastCommonCommitsFinder create(String owner, String repo, String token) {
        return new GithubLastCommonCommitsFinder(owner, repo, token);
    }

}

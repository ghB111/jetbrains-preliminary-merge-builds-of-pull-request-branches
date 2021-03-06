package com.jetbrains.internship.castcommoncommitsfinder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class GithubLastCommonCommitsFinder implements LastCommonCommitsFinder {

    private final String owner;
    private final String repo;

    private final HttpClient httpClient;

    private final HttpRequest.Builder requestBuilderTemplate;

    private final Map<String, CommitSearchColor> commitToColor;
    private final Map<String, List<String>> commitToParents;
    private final Map<String, String> branchToCommit;

    private enum CommitSearchColor {
        REACHED_A,
        REACHED_B,
        REACHED_BOTH
    }

    /**
     * CommitNode class is used to represent information about a node in the git
     * acyclic graph. More precisely, this class tells from which of the two
     * branches (A or B) that node is being reached during a search
     */
    private static class CommitNode {

        private enum CommitSearchColorIntention {
            REACHING_FROM_A,
            REACHING_FROM_B
        }

        public String commit;
        public CommitSearchColorIntention intention;

        CommitNode(String commit, CommitSearchColorIntention intention) {
            this.commit = commit;
            this.intention = intention;
        }
    }

    GithubLastCommonCommitsFinder(String owner, String repo, String token) {

        if (owner == null) {
            throw new IllegalArgumentException("Owner name can't not be null");
        }

        if (repo == null) {
            throw new IllegalArgumentException("Repo name can't not be null");
        }

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        if (token == null) {
            // anonymous access is desired
            requestBuilderTemplate = HttpRequest.newBuilder();
        } else {
            // authentication is to be used
            requestBuilderTemplate = HttpRequest.newBuilder()
                    .header("Authorization", "token " + token);
        }

        requestBuilderTemplate
                .header("Accept", "application/vnd.github.v3+json");

        this.owner = owner;
        this.repo = repo;

        commitToColor = new HashMap<>();
        commitToParents = new HashMap<>();
        branchToCommit = new HashMap<>();
    }

    @Override
    public Collection<String> findLastCommonCommits(String branchA, String branchB) throws IOException {

        String branchCommitA = getCommitByBranch(branchA);
        String branchCommitB = getCommitByBranch(branchB);

        if (branchCommitA.equals(branchCommitB)) {
            Collection<String> lastCommonCommits = new ArrayList<>();
            lastCommonCommits.add(branchCommitA);
            return lastCommonCommits;
        }

        Queue<CommitNode> searchQueue = new ArrayDeque<>();

        commitToColor.put(branchCommitA, CommitSearchColor.REACHED_A);
        commitToColor.put(branchCommitB, CommitSearchColor.REACHED_B);

        for (String commit : new String[]{branchCommitA, branchCommitB}) {

            List<String> parents = getCommitParents(commit);

            List<CommitNode> parentNodes;
            if (commit.equals(branchCommitA)) {
                parentNodes = parents.stream()
                        .map(x -> new CommitNode(x, CommitNode.CommitSearchColorIntention.REACHING_FROM_A))
                        .collect(Collectors.toList());
            } else {
                parentNodes = parents.stream()
                        .map(x -> new CommitNode(x, CommitNode.CommitSearchColorIntention.REACHING_FROM_B))
                        .collect(Collectors.toList());
            }

            searchQueue.addAll(parentNodes);
        }

        Collection<String> lastCommonCommits = new ArrayList<>();

        while (!searchQueue.isEmpty()) {
            CommitNode commitNode = searchQueue.remove();

            if (updateCommitNodeColor(commitNode)) {
                deleteReachableCommitsFromQueue(commitNode.commit, searchQueue);
                lastCommonCommits.add(commitNode.commit);
                continue;
            }

            if (commitToColor.get(commitNode.commit) == CommitSearchColor.REACHED_BOTH) {
                continue;
            }

            List<String> parents = getCommitParents(commitNode.commit);

            searchQueue.addAll(parents.stream()
                    .map(x -> new CommitNode(x, commitNode.intention))
                    .collect(Collectors.toList()));

        }

        return lastCommonCommits;
    }

    /**
     * During a search, updates the commitToColor map according to the
     * intention during the search.
     * <p>
     * For example, if the node we are about to examine is already reachable
     * from A (can tell using commitToColor), and the intention is to reach
     * it from B, the commitToColor map should be updated to know that
     * this commit is reachable from both A and B.
     *
     * @param commitNode commitNode object which commit's color should be updated
     * @return true, if commitToColor was changed to REACHED_BOTH, false otherwise
     */
    private boolean updateCommitNodeColor(CommitNode commitNode) {

        CommitSearchColor color = commitToColor.get(commitNode.commit);

        if (color == null) {
            commitToColor.put(commitNode.commit,
                    commitNode.intention == CommitNode.CommitSearchColorIntention.REACHING_FROM_A
                            ? CommitSearchColor.REACHED_A
                            : CommitSearchColor.REACHED_B);
            return false;
        }

        if (color == CommitSearchColor.REACHED_A
                && commitNode.intention == CommitNode.CommitSearchColorIntention.REACHING_FROM_B
                || color == CommitSearchColor.REACHED_B
                && commitNode.intention == CommitNode.CommitSearchColorIntention.REACHING_FROM_A) {
            commitToColor.put(commitNode.commit, CommitSearchColor.REACHED_BOTH);
            return true;
        }

        return false;
    }

    /**
     * Performs a dfs to get commits reachable from commit
     * in order to stop unneeded search and to delete
     * those commits from the queue.
     *
     * @param commit commit from which to start the search
     * @param queue  the queue of the search
     */
    private void deleteReachableCommitsFromQueue(String commit, Queue<CommitNode> queue) throws IOException {

        if (queue.isEmpty()) {
            return;
        }

        Set<String> queuePresentCommits = queue.stream()
                .map(x -> x.commit).collect(Collectors.toSet());

        Stack<String> searchStack = new Stack<>();

        searchStack.push(commit);

        while (!searchStack.empty()) {

            String currentCommit = searchStack.pop();

            if (queuePresentCommits.contains(currentCommit)) {
                queue.removeIf(x -> x.commit.equals(currentCommit));
                continue;
            }

            if (commitToColor.get(currentCommit) == CommitSearchColor.REACHED_BOTH) {
                continue;
            }

            List<String> parents = getCommitParents(currentCommit);

            searchStack.addAll(parents);
        }

    }

    /**
     * Returns a list of parents of a commit.
     * <p>
     * If parents of the commit were known before, they are cached, thus are
     * immediately returned. Otherwise, an api call is made to Github in
     * order to retrieve the parents of that commit, as well as 99
     * other commits reachable from it (so, 100 at most).
     * <p>
     * Other commits got from the api call will be cached as well.
     *
     * @param commit SHA of a commit
     * @return list of parents of the given commit.
     */
    private List<String> getCommitParents(String commit) throws IOException {

        if (commitToParents.containsKey(commit)) {
            return commitToParents.get(commit);
        }

        HttpRequest request = requestBuilderTemplate.copy()
                .uri(getGithubAPICommitURI(owner, repo, commit))
                .build();

        String response;
        try {
            response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (InterruptedException e) {
            throw new IOException("Error during Github api call", e);
        }

        JSONArray commitsArray = null;
        try {
            commitsArray = (JSONArray) new JSONParser().parse(response);
        } catch (ParseException e) {
            throw new IOException("Github response parse error", e);
        } catch (ClassCastException e) {
            try {
                String errorMessage = (String) ((JSONObject) new JSONParser().parse(response)).get("message");
                throw new IOException("Github api error: " + errorMessage, e);
            } catch (ParseException ignored) {
            } catch (NullPointerException ee) {
                throw new IOException("Unexpected Github response", ee);
            }
        }

        for (Object commitObj : commitsArray == null ? Collections.emptyList() : commitsArray) {

            String currentCommit;
            JSONArray parentsObjects;
            try {
                currentCommit = (String) ((JSONObject) commitObj).get("sha");
                parentsObjects = (JSONArray) ((JSONObject) commitObj).get("parents");
            } catch (NullPointerException e) {
                throw new IOException("Unexpected Github api response", e);
            }

            List<String> parents = new ArrayList<>(parentsObjects.size());
            for (Object o : parentsObjects) {
                String parentCommit;
                try {
                    parentCommit = (String) ((JSONObject) o).get("sha");
                } catch (NullPointerException e) {
                    throw new IOException("Unexpected Github api response", e);
                }
                parents.add(parentCommit);
            }
            commitToParents.put(currentCommit, parents);
        }

        return commitToParents.get(commit);
    }

    /**
     * Returns a commit SHA associated with the branch name.
     *
     * @param branch name of a branch
     * @return commit SHA
     */
    private String getCommitByBranch(String branch) throws IOException {

        if (branchToCommit.containsKey(branch)) {
            return branchToCommit.get(branch);
        }

        HttpRequest request = requestBuilderTemplate.copy()
                .uri(getGithubAPIBranchReferenceURI(owner, repo, branch))
                .build();

        String response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (InterruptedException e) {
            throw new IOException("Error during Github api call", e);
        }

        JSONObject jo;
        try {
            jo = (JSONObject) new JSONParser().parse(response);
        } catch (ParseException e) {
            throw new IOException("Github response parse error", e);
        }

        String commit;
        try {
            commit = (String) ((JSONObject) jo.get("commit")).get("sha");
        } catch (NullPointerException e) {
            try {
                String errorMessage = (String) jo.get("message");
                throw new IOException("Github api error: " + errorMessage, e);
            } catch (NullPointerException ee) {
                throw new IOException("Unexpected Github api response", ee);
            }
        }

        branchToCommit.put(branch, commit);

        return commit;
    }

    private URI getGithubAPIBranchReferenceURI(String owner, String repo, String branch) {
        return URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/branches/" + branch);
    }

    private URI getGithubAPICommitURI(String owner, String repo, String commit) {
        return URI.create("https://api.github.com/repos/" + owner + "/" + repo
                + "/commits?per_page=100&sha=" + commit);
    }

}

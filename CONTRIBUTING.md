# CONTRIBUTING

The git flow used for changes, in two flavours. Both work on the same repository (`origin`), no fork needed,
and in both `main` is never committed to directly.

## Simple flow (one developer)

1. Branch from an up-to-date `main`:
    ```shell
    git switch main && git pull --ff-only
    git switch -c feature/<short-name>
    ```
2. Commit on the branch, staging only the files of the change:
    ```shell
    git add <files>
    git commit -m "<what the change does>"
    ```
3. Push the branch, so the work is on the remote while `main` stays untouched:
    ```shell
    git push -u origin feature/<short-name>
    ```
4. When the change is ready, bring it into `main` with a fast-forward merge and clean up the branch:
    ```shell
    git switch main && git pull --ff-only
    git merge --ff-only feature/<short-name>
    git push
    git branch -d feature/<short-name>
    git push origin --delete feature/<short-name>
    ```

## Normal flow (multiple contributors)

Steps 1 to 3 are the same as in the simple flow. Then, instead of merging locally:

4. Open a pull request from `feature/<short-name>` into `main`, describing what the change does and how it was tested:
    ```shell
    gh pr create --base main --title "<what the change does>" --body "<what it does and how it was tested>"
    ```
   To see the open pull requests, or the state of the current branch's one:
    ```shell
    gh pr list
    gh pr status
    ```
5. Get the pull request reviewed: at least one approval from another contributor, and green CI checks if any.
   Address the review comments with new commits pushed on the same branch.
6. Rebase-merge the pull request, so `main` keeps a linear history with the commits of the change, and delete the branch:
    ```shell
    gh pr merge <number> --rebase --delete-branch
    ```
   A pull request that is not going to be merged is closed instead:
    ```shell
    gh pr close <number> --delete-branch --comment "<why it is closed>"
    ```
7. Sync locally:
    ```shell
    git switch main && git pull --ff-only
    ```
   `--delete-branch` already removes the local branch too; if it is still there, drop it with
   `git branch -D feature/<short-name>` (`-D`, as the rebased commits on `main` have new hashes).

To enforce this flow, add a branch protection rule on `main` that requires a pull request (and the approvals) before merging.

# Welcome! Thank you for contributing to Apache Pekko Connectors!

We follow the standard GitHub [fork & pull](https://help.github.com/articles/using-pull-requests/#fork--pull) approach to pull requests. Just fork the official repo, develop in a branch, and submit a PR!

You're always welcome to submit your PR straight away and start the discussion (without reading the rest of this wonderful doc, or the README.md). The goal of these notes is to make your experience contributing to Pekko Connectors as smooth and pleasant as possible. We're happy to guide you through the process once you've submitted your PR.

# The Pekko Community

If you have questions about the contribution process or discuss specific issues, we will be happy to try to help via the usual [communication channels](https://github.com/apache/incubator-pekko-connectors?tab=readme-ov-file#community). 

# Contributing to Pekko Connectors

## General Workflow

This is the process for committing code into main.

1. To avoid duplicated effort, it might be good to check the [issue tracker](https://github.com/apache/incubator-pekko-connectors/issues) and [existing pull requests](https://github.com/apache/incubator-pekko-connectors/pulls) for existing work.
   - If there is no ticket yet, feel free to [create one](https://github.com/apache/incubator-pekko-connectors/issues/new) to discuss the problem and the approach you want to take to solve it.

1. Perform your work according to the [pull request requirements](#pull-request-requirements).

1. When the feature or fix is completed you should open a [Pull Request](https://help.github.com/articles/using-pull-requests) on [GitHub](https://github.com/apache/incubator-pekko-connectors/pulls). Prefix your PR title with a marker to show which module it affects (eg. "JMS", or "AWS S3").

1. The Pull Request should be reviewed by other maintainers (as many as feasible/practical). Outside contributors are encouraged to participate in the review process, it is not a closed process.

1. After the review you should fix the issues (review comments, CI failures, compiler warnings) by pushing a new commit for new review, iterating until the reviewers give their thumbs up and CI tests pass.

1. If the branch merge conflicts with its target, rebase your branch onto the target branch.

## Running tests

| :exclamation:  Take care when running the shell scripts in this repo. |
|-----------------------------------------------------------------------|

You can run tests using [sbt](https://www.scala-sbt.org/). With this repo, you will typically be working on one connector at a time. For instance, the FTP connector is in the `ftp` folder and you can run its tests with `sbt ftp/test`. You should read the rest of this section before running these tests.

This repo is for connectors that integrate with 3rd party services (e.g. AWS S3, FTP, Hive). For many connectors, you will need to use [Docker Compose](https://docs.docker.com/compose/) to start servers that the Pekko Connector tests will need to interact with. The tests don't expect to interact with live resources but instead expect to work with local services that provide the right functionality.

You can get an idea of what Docker commands that you need to run tests for specific connectors by looking at the GitHub Actions workflow [check-build-test.yml](https://github.com/apache/incubator-pekko-connectors/blob/75e9a4867eec3e1c2b971eb7e13a0f0b9dbddab3/.github/workflows/check-build-test.yml#L78-L125).

The Docker setup in many cases requires the use of shell scripts that are designed to run inside Docker containers and are not designed for users to be running on their own machines. Please take care when running any shell scripts in this repo.

To continue with the FTP connector as an example, you will need to run this [script](https://github.com/apache/incubator-pekko-connectors/blob/main/scripts/ftp-servers.sh) (that runs Docker Compose commands) before running the tests.

```
./scripts/ftp-servers.sh
```

This FTP setup does not work well on Apple Macs. There is a workaround described by Sebastien Alfers in this [write up](https://github.com/sebastian-alfers/commons-net/blob/1cd10e1da577d5f900c5e33af2a041de1361eb25/README_REPRODUCE.md#squid--ftp-on-mac).

## Pekko Connectors specific advice

We've collected a few notes on how we would like Pekko Connectors modules to be designed based on what has evolved so far.
Please have a look at our [contributor advice](contributor-advice.md).

## Pull Request Requirements

For a Pull Request to be considered at all it has to meet these requirements:

1. Pull Request branch should be given a unique descriptive name that explains its intent. Prefix your PR title with a marker to show which module it affects (eg. "JMS", or "AWS S3").

1. Refer to issues it intends to fix by adding "Fixes #{issue id}" to the notes.

1. Code in the branch should live up to the current code standard:
   - Not violate [DRY](https://www.oreilly.com/library/view/97-things-every/9780596809515/ch30.html).
   - [Boy Scout Rule](https://www.oreilly.com/library/view/97-things-every/9780596809515/ch08.html) needs to have been applied.

1. Regardless if the code introduces new features or fixes bugs or regressions, it must have comprehensive tests.

1. The code must be well documented (see the [Documentation](contributor-advice.md#documentation) section).

1. The commit messages must properly describe the changes, see [further below](#creating-commits-and-writing-commit-messages).

1. Do not use ``@author`` tags since it does not encourage [Collective Code Ownership](http://www.extremeprogramming.org/rules/collective.html). Contributors get the credit they deserve in the release notes.

If these requirements are not met then the code should **not** be merged into 'main' branch, or even reviewed - regardless of how good or important it is. No exceptions.


## Creating Commits And Writing Commit Messages

Follow these guidelines when creating public commits and writing commit messages.

1. First line should be a descriptive sentence what the commit is doing. It should be possible to fully understand what the commit does — but not necessarily how it does it — by just reading this single line. We follow the “imperative present tense” style for commit messages ([more info here](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)).

   It is **not ok** to only list the ticket number, type "minor fix" or similar.
   If the commit is a small fix, then you are done. If not, go to 3.

1. Following the single line description should be a blank line followed by an enumerated list with the details of the commit.

1. Add keywords for your commit (depending on the degree of automation we reach, the list may change over time):
    * ``Review by @gituser`` - if you want to notify someone on the team. The others can, and are encouraged to participate.

Example:

    Add eventsByTag query #123

    * Details 1
    * Details 2
    * Details 3

## Applying code style to the project

The project uses [scalafmt](https://scalameta.org/scalafmt/) to ensure code quality which is automatically checked on
every PR. If you would like to check for any potential code style problems locally you can run `sbt checkCodeStyle`
and if you want to apply the code style then you can run `sbt applyCodeStyle`.

### Ignoring formatting commits in git blame

Throughout the history of the codebase various formatting commits have been applied as the scalafmt style has evolved over time, if desired
one can setup git blame to ignore these commits. The hashes for these specific are stored in [this file](.git-blame-ignore-revs) so to configure
git blame to ignore these commits you can execute the following.

```shell
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

## How To Enforce These Guidelines?

1. [GitHub actions](https://github.com/apache/incubator-pekko-connectors/actions) automatically merge the code, builds it, runs the tests and sets Pull Request status accordingly of results in GitHub.
1. [Scalafmt](http://scalameta.org/scalafmt/) enforces some of the code style rules.
1. [sbt-header plugin](https://github.com/sbt/sbt-header) manages consistent copyright headers in every source file.
1. Enabling `fatalWarnings := true` for all projects.

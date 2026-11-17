# Pekko Stream Contrib

This is a fork of [akka-stream-contrib](https://github.com/akka/akka-stream-contrib).

This project provides a home to Pekko Streams add-ons which does not fit into the core Pekko Streams module. There can be several reasons for it to not be included in the core module, such as:

- the functionality is not considered to match the purpose of the core module
- it is an experiment or requires iterations with user feedback before including into the stable core module
- it requires a faster release cycle

> [!NOTE]
> This repository is not released as a binary artifact and is only shared as sources.

## Branches

- `main` depends on the [Pekko](https://pekko.apache.org/) 2.0.0 release (Scala 2.13 and 3, Java 17+).
- [`pekko1`](https://github.com/pjfanning/pekko-stream-contrib/tree/pekko1) keeps Pekko 1.x support (currently Pekko 1.7.1, including Scala 2.12 and Java 11).

Moving from Pekko 1 to Pekko 2 needed only a small code change. See [#7](https://github.com/pjfanning/pekko-stream-contrib/pull/7/files) for the full diff.

## Caveat Emptor

> [!WARNING]
> A component in this project does not have to obey the rule of staying binary compatible between releases. Breaking API changes may be introduced without notice as we refine and simplify based on your feedback. A module may be dropped in any release without prior deprecation.

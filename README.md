Pekko Stream Contrib
===================

This is a fork of https://github.com/akka/akka-stream-contrib

This project provides a home to Pekko Streams add-ons which does not fit into the core Pekko Streams module. There can be several reasons for it to not be included in the core module, such as:

* the functionality is not considered to match the purpose of the core module
* it is an experiment or requires iterations with user feedback before including into the stable core module
* it requires a faster release cycle

**This repository is not released as a binary artifact and only shared as sources.**

Caveat Emptor
-------------

A component in this project does not have to obey the rule of staying binary compatible between releases. Breaking API changes may be introduced without notice as we refine and simplify based on your feedback. A module may be dropped in any release without prior deprecation.

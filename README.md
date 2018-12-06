# Business Register API Common
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](./LICENSE)

This project contains common code used across Business Register APIs.


### Development Tasks

Run unit tests with coverage:

    sbt clean coverage test coverageReport

Generate static analysis report:

    sbt scapegoat

Publish to the local Ivy repository:

    sbt clean publishLocal


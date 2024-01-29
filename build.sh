#!/usr/bin/env bash

sbt clean scalafmtAll scalafmtSbt compile test:compile it:compile

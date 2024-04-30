#!/usr/bin/env bash

sbt clean scalafmtAll scalafmtSbt scalafmtCheckAll compile test:compile it:compile

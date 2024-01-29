#!/usr/bin/env bash

sbt scalastyle scalafmtCheckAll test:scalafmtCheckAll test it:test

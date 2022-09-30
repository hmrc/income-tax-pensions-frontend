#!/usr/bin/env bash

sbt scalastyle clean compile coverage test it:test coverageReport

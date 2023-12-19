#!/usr/bin/env bash
#loads environment variables from `.env` file
(set -a && source '.env' && set +a && sbt run)
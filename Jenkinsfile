#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */

buildPlugin(
  useContainerAgent: true,
  configurations: [
    [platform: 'linux', jdk: 21]
    [platform: 'windows', jdk: 17]
])

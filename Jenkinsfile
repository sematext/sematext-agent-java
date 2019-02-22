properties(
  [
    buildDiscarder(
      logRotator(
        daysToKeepStr: '7',
        numToKeepStr: '5',
        artifactNumToKeepStr: '5'
      )
    ),
    disableConcurrentBuilds()
  ]
)

def label = "worker-${UUID.randomUUID().toString()}"

podTemplate(label: label, serviceAccount: 'jenkins', containers: [
  containerTemplate(name: 'jnlp', image: 'jenkins/jnlp-slave:alpine', args: '${computer.jnlpmac} ${computer.name}'),
  containerTemplate(name: 'maven', image: 'maven:3.5.3-jdk-8', command: 'cat', ttyEnabled: true, alwaysPullImage: true),
],
podRetention: never()) {
  node(label) {
    def sematextAgentJava = checkout ([
        $class: 'GitSCM',
        branches: scm.branches,
        userRemoteConfigs: scm.userRemoteConfigs,
        extensions: [
          [$class: 'LocalBranch', localBranch: "**"],
          [$class: 'CloneOption', noTags: true, depth: 50, shallow: true],
          [$class: 'RelativeTargetDirectory', relativeTargetDir: 'sematext-agent-java'],
        ]
      ])
    def gitBranch = sematextAgentJava.GIT_LOCAL_BRANCH

    stage('Prereqs') {
      try {
        container('maven') {
          sh """
            apt-get update
            apt-get install -y --no-install-recommends thrift-compiler
            """
        }
      }
      catch (exc) {
        throw(exc)
      }
    }

    stage('Build') {
      try {
        container('maven') {
          sh """
            cd sematext-agent-java
            mvn --batch-mode clean install -DskipTests
            """
        }
      }
      catch (exc) {
        throw(exc)
      }
    }

    stage('Test') {
      try {
        container('maven') {
          sh """
            cd sematext-agent-java
            mvn --batch-mode test
            """
        }
      }
      catch (exc) {
        throw(exc)
      }
    }
  }
}

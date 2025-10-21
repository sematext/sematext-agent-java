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

podTemplate(
  label: label, 
  serviceAccount: 'jenkins', 
  namespace: 'jenkins', 
  yaml: """
apiVersion: v1
kind: Pod
spec:
  priorityClassName: sematext-app-important
""",
  containers: [
    containerTemplate(name: 'jnlp', image: 'jenkins/inbound-agent', args: '${computer.jnlpmac} ${computer.name}'),
    containerTemplate(name: 'maven', image: 'sematext/maven:latest', command: 'cat', ttyEnabled: true, alwaysPullImage: false),
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

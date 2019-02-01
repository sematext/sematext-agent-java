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
  containerTemplate(name: 'jnlp', image: 'sematext/jnlp-slave-git-lfs:alpine', args: '${computer.jnlpmac} ${computer.name}'),
  containerTemplate(name: 'docker', image: 'sematext/docker:latest', command: 'cat', ttyEnabled: true, alwaysPullImage: true),
  containerTemplate(name: 'maven', image: 'maven:3.5.3-jdk-8', resourceRequestCpu: '2', command: 'cat', ttyEnabled: true, alwaysPullImage: true),
],
volumes: [
  hostPathVolume(mountPath: '/tmp/cache', hostPath: '/tmp/jenkins/cache'),
  hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
],
podRetention: never()) {
  node(label) {
    // Checkout repo in $WORKSPACE/sematext-agent-java directory
    def sematextCloud = checkout ([
        $class: 'GitSCM',
        branches: scm.branches,
        userRemoteConfigs: scm.userRemoteConfigs,
        extensions: [
          [$class: 'LocalBranch', localBranch: "**"],
          [$class: 'CloneOption', noTags: true, depth: 50, shallow: true],
          [$class: 'RelativeTargetDirectory', relativeTargetDir: 'sematext-agent-java'],
        ]
      ])

    def gitCommit = sematextCloud.GIT_COMMIT
    def gitBranch = sematextCloud.GIT_LOCAL_BRANCH
    def shortGitCommit = "dev-${gitCommit[0..10]}"
    def gitCommitPrevious = sematextCloud.GIT_PREVIOUS_SUCCESSFUL_COMMIT

    stage('Prepare go deps') {
      try {
        container('maven') {
          sh """
            cd sematext-agent-java
            apt-get update
            apt-get -y --no-install-recommends install automake bison flex g++ git libboost-all-dev libevent-dev libssl-dev libtool make pkg-config
            wget http://www.us.apache.org/dist/thrift/0.9.3/thrift-0.9.3.tar.gz
            tar xfz thrift-0.9.3.tar.gz
            cd thrift-0.9.3
            ./configure --enable-libs=no
            make install
            cd ..
            mvn clean install
            """
        }
      }
      catch (exc) {
        throw(exc)
      }
    }

  }
}

#!/usr/bin/groovy
def call(body, label="mypod") {
  podTemplate(label: label,
//        containers: [containerTemplate(name: 'maven', image: 'maven', command: 'cat', ttyEnabled: true)],
//        volumes: [secretVolume(secretName: 'maven-settings', mountPath: '/root/.m2'),
//                  persistentVolumeClaim(claimName: 'maven-local-repo', mountPath: '/root/.m2nrepo')]) {
// FIXME: should add maven setting and better to add local repo as pv
        containers: [containerTemplate(name: 'maven', image: 'maven:3.5-jdk-8-alpine', command: 'cat', ttyEnabled: true)]){
          body()
        }
}

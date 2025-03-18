# Setup Jenkins and a pipeline

## Guidelines: 
https://www.docker.com/blog/docker-and-jenkins-build-robust-ci-cd-pipelines/


## Build
`docker build -t acp-jenkins:lts .`

This will create the Jenkins structure, etc. 


**Now the plugins can be loaded:**

- Docker Pipeline Plugin
- Docker
- CloudBees Docker Build and Publish

**Next steps**
- Now set the maven tool
- Specify docker hub connectivity

Then the pipeline can be created and run:  [jenkins-pipeline](jenkins-pipeline)

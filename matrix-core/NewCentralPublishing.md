# New Central Publishing

https://central.sonatype.org/publish/publish-portal-api/#verify-status-of-the-deployment

list all tasks
./gradlew -q :matrix-core:tasks --all

gradlew clean signMavenPublication
- zip all in libs + publications/pom-default.xml
  se
    alipsa
      matrix
        core
          matrix-core
            3.4.0
              pom-default.xml
              pom-default.xml.asc
              matrix-core-3.4.0-sources.jar
              matrix-core-3.4.0-sources.jar.asc
              matrix-core-3.4.0-javadoc.jar
              matrix-core-3.4.0-javadoc.jar.asc
              matrix-core-3.4.0.jar
              matrix-core-3.4.0.jar.asc
  (md5 files are not needed)
- create the bearer token
  $ printf "example_username:example_password" | base64
- upload the bundle
```
  $ curl --request POST \
  --verbose \
  --header 'Authorization: Bearer ZXhhbXBsZV91c2VybmFtZTpleGFtcGxlX3Bhc3N3b3Jk' \
  --form bundle=@central-bundle.zip \
  https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC
```
note The value that is returned is the deployment ID

- Check status
```
$ curl --request POST \
  --verbose \
  --header 'Authorization: Bearer ZXhhbXBsZV91c2VybmFtZTpleGFtcGxlX3Bhca3N3b3JkCg==' \
  'https://central.sonatype.com/api/v1/publisher/status?id=28570f16-da32-4c14-bd2e-c1acc0782365'
```

Check status until it is either PUBLISHED or FAILED. If it is FAILED, you can check the error message in the response.
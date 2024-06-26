# Netflix API Read Cache
This is a spring boot applicable using Maven build. It's build using Vert.x (NIO) tool kit to get advantage of high throughout w/o any blocking operation overhead. This service uses Redis as local cache to keep all paginated response in a flat structure (Set) and generate a leader board for Bottom N query. For some API request it simply works as Proxy to Git API, while it also provide its own flavor of custom views API on top of all cached Netflix repo info. 

* Environment Variable
  * APP_PORT (default 8080)
  * GITHUB_API_TOKEN (default empty)
  
![Screenshot 2023-11-13 at 23 07 04](https://github.com/mohitmahi/Netflix/assets/37902584/8c1f2aad-0d4c-4f5c-943a-5bacf15fbd89)


## API Path
#### 1) Local Cached in Redis (periodic refresh)
   * /
   * /orgs/Netflix
   * /orgs/Netflix/members
   * /orgs/Netflix/repos
#### 2) Custom View in Redis (sorted set)
   * /view/bottom/:N/forks
   * /view/bottom/:N/open_issues
   * /view/bottom/:N/stars
   * /view/bottom/:N/last_updated
#### 3) Proxy for any https://api.github.com/ path
   * All except above 1) and 2)
#### 4) Health Check
   * /healthcheck


## Tech Stack
* Spring Boot Application
* Maven
* Redis 
* Vert.x toolkit for web app
 * ![1_pLDxmoOkca6m-koBvn6-qg](https://github.com/mohitmahi/Netflix/assets/37902584/b5740693-653b-4044-9ecd-66c543df3d8a)

## Redis Snapshot with Leaderboard
![Screenshot 2023-11-13 at 22 10 45](https://github.com/mohitmahi/Netflix/assets/37902584/ed6cfbfa-35af-4492-87bb-fda1f7eedc0c)


## Test (86%)
<img width="1891" alt="Screenshot 2023-11-13 at 22 34 42" src="https://github.com/mohitmahi/Netflix/assets/37902584/bec9ce7f-3388-4c82-bfa7-12319512af10">

## 2 Failing Test for bottom/N/stars

*  https://github.com/Netflix/iceberg-python now have 1 star count, so below test expected output are incorrect. 
## -n test-06-07: /view/bottom/5/stars = 
failed
  >>   expected=[["Netflix/dgs-examples-kotlin-2.7",0],["Netflix/iceberg-python",0],["Netflix/octodns-ns1",0],["Netflix/octodns-route53",0],["Netflix/virtual-kubelet",0]]

  >> response=[["Netflix/octodns-ultra",1],["Netflix/dgs-examples-kotlin-2.7",0],["Netflix/octodns-ns1",0],["Netflix/octodns-route53",0],["Netflix/virtual-kubelet",0]]

## -n test-06-08: /view/bottom/10/stars = 
failed
>>   expected=[["Netflix/conductor-docs",1],["Netflix/dgs-examples-java.latest",1],["Netflix/eclipse-mat",1],["Netflix/octodns",1],["Netflix/octodns-ultra",1],["Netflix/dgs-examples-kotlin-2.7",0],["Netflix/iceberg-python",0],["Netflix/octodns-ns1",0],["Netflix/octodns-route53",0],["Netflix/virtual-kubelet",0]]

>>   response=[["Netflix/octodns-ultra",1],["Netflix/octodns",1],["Netflix/iceberg-python",1],["Netflix/eclipse-mat",1],["Netflix/dgs-examples-java.latest",1],["Netflix/conductor-docs",1],["Netflix/dgs-examples-kotlin-2.7",0],["Netflix/octodns-ns1",0],["Netflix/octodns-route53",0],["Netflix/virtual-kubelet",0]]

## 2 Failing Test for bottom/N/last_updated
* https://github.com/Netflix/mantis-api last updated in expected response is invalid so test are failing. 


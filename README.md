# Netflix API Read Cache
* Environment Variable
  * APP_PORT (default 8080)
  * GITHUB_API_TOKEN (default empty)


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

## Test (86%)
![Screenshot 2023-11-13 at 04 51 44](https://github.com/mohitmahi/Netflix/assets/37902584/78d3002b-dfe2-40d7-8282-c4975473957f)
## Failing Test 
## -n test-06-07: /view/bottom/5/stars = 
* https://github.com/Netflix/iceberg-python now have 1 star count, so test expected output is incorrect. 
failed
>>   expected=[["Netflix/dgs-examples-kotlin-2.7",0],["Netflix/iceberg-python",0],["Netflix/octodns-ns1",0],["Netflix/octodns-route53",0],["Netflix/virtual-kubelet",0]]

  >> response=[["Netflix/octodns-ultra",1],["Netflix/dgs-examples-kotlin-2.7",0],["Netflix/octodns-ns1",0],["Netflix/octodns-route53",0],["Netflix/virtual-kubelet",0]]

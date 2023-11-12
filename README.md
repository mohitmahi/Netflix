# Netflix API Read Cache
* Environment Variable
  * APP_PORT (default 8080)
  * GITHUB_API_TOKEN (default empty)


## API Path
####1) Local Cached in Redis (periodic refresh)
   * /
   * /orgs/Netflix
   * /orgs/Netflix/members
   * /orgs/Netflix/repos
####2) Custom View in Redis (sorted set)
   * /view/bottom/:N/forks
   * /view/bottom/:N/open_issues
   * /view/bottom/:N/stars
   * /view/bottom/:N/last_updated
####3) Proxy for any https://api.github.com/ path
   * All except above 1) and 2)
####4) Health Check
   * /healthcheck


## Tech Stack
* Spring Boot Application
* Maven
* Redis 
* Vert.x toolkit for web app
 * ![1_pLDxmoOkca6m-koBvn6-qg](https://github.com/mohitmahi/Netflix/assets/37902584/b5740693-653b-4044-9ecd-66c543df3d8a)

## Test
<img width="571" alt="Screenshot 2023-11-12 at 15 38 14" src="https://github.com/mohitmahi/Netflix/assets/37902584/6ac5a524-7238-4004-bc88-c351a46d019f">

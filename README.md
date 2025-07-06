**CryptoAggregatorLocalDemo** is a simple API designed to be locally executed and hosted.  It is built on Spring Boot 3.x, built with Maven and requires JDK 21+ to run.  Cache is implemented as a Docker Redis container, and the database is Spring JPA and H2 in-memory SQL.

## Project Setup
Clone the repository at:
https://github.com/wangcary19/CryptoAggregatorLocalDemo 

### Application Configuration
Various settings are available in the *application.properties* file.  Configure the below according 

*crypto.api.key*: API key required for accessing CoinGeckoAPI
<br>
*crypto.prefetch.limit*: changes the amount of days worth of data the application prefetches from CoinGeckoAPI at startup
<br>
*rate.limiter.limit*: changes the amount of requests allowed per minute
<br>
<br>
Note that because the database and cache are both hosted in-memory, it may be necessary to increase the allotted memory amount by the hosting service.






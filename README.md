**CryptoAggregatorLocalDemo** is a simple API designed to be locally executed and hosted.  It is built on Spring Boot 3.x, built with Maven, and requires JDK 21+ to run.  Cache is implemented as a Docker Redis container, and the database is Spring JPA and H2 in-memory SQL.

Data is derived from the CoinGeckoAPI, which due to pricing restrictions, is only able to serve data from within the past 365 days.

This project was written with the help of LLMs, with the human-written to AI-written split being approximately 70/30.

# Project Setup
Clone the repository at:
https://github.com/wangcary19/CryptoAggregatorLocalDemo 

### Application Configuration
Various settings are available in the *application.properties* file.  Configure the following before startup:

*crypto.api.key*: API key required for accessing CoinGeckoAPI
<br>
*crypto.prefetch.limit*: changes the amount of days worth of data the application prefetches from CoinGeckoAPI at startup
<br>
*rate.limiter.limit*: changes the amount of requests allowed per minute
<br>
<br>
Note that because the database and cache are both hosted in-memory, it may be necessary to increase the allotted memory amount by the hosting service (in IntellJ, search "Change Memory Settings" and increase the allotted amount ot 8192MB/8GB, if possible).

In production, these settings should be externalized and managed at a global level.

### Running the Application
The below instructions assume the user is running the project in IntelliJ.  Please note behavior may be different on other IDEs, where Maven support is non-native.

1) Load the project in a new workspace, and ensure that the project JDK version is set to Java 21 or higher (on Mac, view the project structure settings with "Cmd + ;")
2) IntelliJ will auto-detect the Maven project, and attempt to download the required dependencies in the background.  Open the Maven panel on the right, open Lifecycle, then right-click on any pre-existing configuration and select "Modify Run Configuration..."
3) In the proceeding menu, enter any name for the Run Configuration profile, then place the following the command: "**clean install -X -U -f pom.xml**"
4) Save and execute the Run Configuration.  If successful, a "target" directory should appear in the project folder.
5) Start the application by navigating to *CryptoAggregatorLocalDemoApplication.class* and pressing run on the main() function.  Alternatively, a Spring Boot profile should auto-populate in the top-right Run menu; select either to run the application.
6) On launch, Spring Boot will automatically generate a user password to perform a simple logon to the system.  This will be viewable in the console output.  Note that the default username is "user"

The default port is localhost:8080.

# Documentation
The application is accessed via six endpoints, four of which return data and two which are used to reset the cache and database, respectively.
## Endpoints
- **GET localhost:8080/current-prices/{ids}:** returns the most recent available prices for coins, where 
  - {ids} is the comma-separated identifiers for cryptocurrencies (e.g. "bitcoin", "ethereum")
  - Returns a JSON in the form {coin_id : price} for each cryptocurrency requested
- **GET localhost:8080/past-price/{id}/{date}:** returns the price of a coin on a specified date, where 
  - {id} is the identifier for a cryptocurrency (e.g. "bitcoin") and 
  - {date} is the desired date in "DD-MM-YYYY" format
  - Returns a JSON in the form {coin_id : price}
- **GET localhost:8080/history/{id}/{fromDate}/{toDate}:** returns the prices of a coin over a specified date range, where 
  - {id} is the identifier for a cryptocurrency (e.g. "bitcoin"), 
  - {fromDate} is the date from which data retrieval should begin in "DD-MM-YYYY" format, and 
  - {toDate} is the date up to which data retrieval should end in "DD-MM-YYYY" format
  - Returns a JSON in the form {timestamp : price} for each time entry within the interval
- **GET localhost:8080/ping:** returns a simple JSON response, useful for monitoring the health of the application
  - Returns a JSON in the form {status : message}
- **POST localhost:8080/system/clear-cache:** clears all stored data from the Redis instance
- **POST localhost:8080/system/clear-database:** clears all stored data from the in-memory H2 instance
  
Note that in a production environment, both POST endpoints should be disabled, or protected by an ABAC/RBAC filter that checks for sufficient user permissions.

# Architecture
## Data Management and Storage
CoinGeckoAPI does not publish a JSON schema and in fact returns dynamic response bodies dependent on the data requested.  As a result, it is not possible to define a data-contracted object for parsers to auto-correlate JSON with Java class field values.  A classic CRUD-based implementation may thus not fully cover the data requirements.

Instead, an Entity object of *Asset.java* is defined with the relevant fields to store the required data.  These fields are cryptocurrency ID, price in USD, timestamp in UNIX epoch milliseconds, and a special key field that combines the ID and timestamp fields to allow for hashing when caching and storing to the database.

For long-term storage, a single SQL table with columns corresponding to the aforementioned fields and the special key as the primary key was used.  This is effective as a solution in local environments, but in production a timeseries database will be preferable for performance reasons (notably, easier sharding).  In addition, a separate database management service should be built to auto-aggregate, flush, and populate the database as needed, independent of the application.

## Pre-fetching, Caching, and General Optimization
The application performs two pre-fetches at startup:
<br>
1) Grabs an updated list of all valid cryptocurrency IDs
2) Grabs entries for Bitcoin and Ethereum from the past X days, where X is defined in the *crypto.prefetch.limit* setting

When calling any of the GET endpoints, the application first validates the requested IDs against the allowed list.  Then, it checks the Redis cache for the hashed request ID and timestamp.  If not present, it will check the database for the hashed request ID and timestamp.  Finally, if not found in either cache or database, a request is assembled and made to the CoinGeckoAPI.

Note that the implementation is limited to times in a "DD-MM-YYYY" format.  In the future, this can be expanded to allow inputs in Unix epoch time and/or "DD-MM-YYYY HH:MM:SS" format.

There is a granularity limitation brought on by the CoinGeckoAPI, which serves different time interval data depending on how long ago the requested data originated.  In a production environment, this should be accounted for by a filter/sieve that returns results only of a user-specified interval. 

## Exception Handling
Exception handling is done by *GlobalExceptionHandler.java*, which throws a *CryptoAggregatorException.java*, an extension of Java's native Runtime Exception.  This halts all API processes once thrown, and retrieves an appropriate error message from *errors.properties* to display in the response body.

The appropriate HTTP return codes are automatically assigned by Spring Boot.

In a production environment, these exception events should be linked to a unified logging (Splunk) and monitoring (cloud dashboard) service.
## Rate Limiting
Rate limiting is handled by a filter that intercepts all requests to the application, located at *RateLimiterFilter.java* and configured/registered at *RateLimiterConfig.java* as a Bean.  It maintains a user IP to atomic request number map that is refreshed every minute, and returns HTTP 429 if a request from an IP exceeds the defined limit.

In a production environment, rate limiting should not be implemented at the application level, and should instead sit on the proxy.  The proxy rate limiter should also be linked to a monitoring (cloud dashboard) service.
## Health and Monitoring
The application health can be queried via a ping endpoint (see above), which returns a simple JSON body.

The application also monitors the health of the upstream CoinGeckoAPI; every five minutes, it attempts to ping the health check endpoint of CoinGeckoAPI, and will throw an exception if unreachable.

In production, the monitoring of the upstream API should be decoupled from the application and handled by a micro/function-less service.
## Security
Security is auto-handled by Spring Boot, with a simple logon screen and a default username with session-persistent password.

In production, security must be robust and able to cater to a variety of authentication and authorization paradigms, for users within and external to an organization.  Accounts should utilize an OAuth 2.0-compliant protocol (or LDAP, if necessary for legacy compatibility) and should bear custom authorization definitions, whether role-based (RBAC) or attribute-based (ABAC).  These policies must be defined at the global governance level, with a centralized/federated account management and session management service.  The application must ingest and pass a valid token between all requests and transactions to ensure continuous security; in Spring, this is done through an inversion-of-control filter chain, which can be custom-configured via Bean registrations.

# Areas of Improvement and TO-DOs
## Pagination
API requests can quickly scale in size, especially if retrieving data for long periods of time.  Pagination should be implemented on large database read requests, to improve efficiency and prevent memory overloading.
## Timestamp and Primary Key Rounding
The database currently records timestamps in exact milliseconds.  Given the minimum granularity of five minutes provided by CoinGeckoAPI, these timestamps can be rounded to the nearest 3000 milliseconds, to simulate perfect granularity.  This will make cache hits and database read calls more likely, minimizing costs from calling the upstream service.
## Testing
Testing scope is limited to components and runs only at build time.  More robust testing, in the form of automated external testing, should be implemented.  Especially critical are outage simulations, load tests, and performance tests, which are not implementable within the standalone application.
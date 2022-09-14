# Daml Testament

[Ethereum Testament](../ethereum/) ported to Daml. 
Personal sample repo to learn different Daml&Canton features.

<!-- TODO: add diagram -->

Network consists of 3 organizations:

* Provider - issue/update/revoke testaments
* Bank - holds user tokens; distributes them on testament execution
* Government - confirms all operations; announces testament execution

Repo consists of 6 directories:

* [auth-server](./auth-server/) - sample Ktor server for providing valid JWS/JWKS
* [bank-gateway](./bank-gateway) - JSON-RPC Node gateway to network for bank services
* [contracts](./contracts/) - Daml templates
* [gov-app](./gov-app) - governmental React application
* [network](./network/) - files required to run docker compose network
* [provider-gateway](./provider-gateway/) - REST Quarkus gateway to network for provider services

## Local deployment

Docker compose configuration is provided. 
So, to start all services issue `docker compose up`. Batteries included.

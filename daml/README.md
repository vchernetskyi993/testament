# Daml Testament

[Ethereum Testament](../ethereum/) ported to Daml. 
Personal sample repo to learn different Daml&Canton features.

<!-- TODO: add diagram -->

Network consists of 3 organizations:

* Provider - issue/update/revoke testaments
* Bank - holds user tokens; distributes them on testament execution
* Government - confirms all operations; announces testament execution

Repo consists of 6 directories:

* auth-server - sample Ktor server for providing valid JWS/JWKS
* bank-gateway - JSON-RPC Node gateway to network for bank services
* contracts - Daml templates
* gov-app - governmental React application
* network - files required to run docker compose network
* provider-gateway - REST Quarkus gateway to network for provider services

## Local deployment

Docker compose configuration is provided. 
So, to start all services issue `docker compose up`. Batteries included.

## Used development resources:

### Common

https://github.com/digital-asset/ex-secure-daml-infra

---

#### Auth server

https://connect2id.com/products/nimbus-jose-jwt
https://docs.daml.com/canton/usermanual/static_conf.html#jwt-authorization
https://docs.daml.com/tools/sandbox.html#run-with-authorization
https://docs.daml.com/app-dev/authorization.html#user-access-tokens


#### Canton (domain+nodes)

https://docs.daml.com/canton/tutorials/getting_started.html
https://docs.daml.com/canton/usermanual/docker.html
https://hub.docker.com/r/digitalasset/canton-open-source

#### JSON-RPC

https://hub.docker.com/r/digitalasset/daml-sdk

---

### Government

https://www.npmjs.com/package/@daml/react
https://docs.daml.com/app-dev/bindings-ts/daml2js.html


#### Nginx

https://hub.docker.com/_/nginx
http://nginx.org/en/docs/beginners_guide.html#conf_structure

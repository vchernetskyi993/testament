Build images:

```shell
docker build ../auth-server/ -t testament/auth-server
```

Load images to Minikube:

```shell
minikube image load testament/auth-server
```

Deploy with defaults:

```shell
tk apply environments/default
```

Deploy with overrides:

```shell
tk apply environments/default --tla-str postgresGovPassword=postgres0
```

For available overrides see [base.libsonnet](lib/base.libsonnet) `platform` function parameters.

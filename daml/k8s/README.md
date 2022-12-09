Build images:

```shell
./build.sh
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

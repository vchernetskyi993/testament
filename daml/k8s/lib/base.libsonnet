local k = import 'k.libsonnet';
local postgres = import 'postgres.libsonnet';
local tk = import 'tk';

local namespace = k.core.v1.namespace;

{
  platform(
    govPostgresPassword='postgres',
    providerPostgresPassword='postgres',
    bankPostgresPassword='postgres',
  ):: {
    namespace: namespace.new(tk.env.spec.namespace),

    // Government
    'postgres.gov': postgres.new(
      image=$._config.postgres.image,
      org='gov',
      password=govPostgresPassword,
      databases=['domain', 'government_ledger', 'government_json'],
    ),
    domain: {},
    'ledger.gov': {},  // << contract-builder
    'json.gov': {},
    'auth.gov': {},
    'nginx.gov': {},  // << ui-builder

    // Provider
    'postgres.provider': postgres.new(
      image=$._config.postgres.image,
      org='provider',
      password=providerPostgresPassword,
      databases=['provider_ledger', 'provider_json'],
    ),
    'ledger.provider': {},  // << contract-builder
    'json.provider': {},
    'auth.provider': {},
    'gateway.provider': {},

    // Bank
    'postgres.bank': postgres.new(
      image=$._config.postgres.image,
      org='bank',
      password=bankPostgresPassword,
      databases=['bank_ledger', 'bank_json'],
    ),
    'ledger.bank': {},  // << contract-builder
    'json.bank': {},
    'auth.bank': {},
    'gateway.bank': {},

    // depends on all services above; can be done using k8s?
    'create-factory': {},
  },
}
+ (import 'config.libsonnet')

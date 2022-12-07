local postgres = import 'components/postgres.libsonnet';
local k = import 'k.libsonnet';
local tk = import 'tk';
local auth = import 'components/auth.libsonnet';
local domain = import 'components/domain.libsonnet';

local namespace = k.core.v1.namespace;

{
  platform(
    govPostgresPassword='postgres',
    govAuthUser='govadmin',
    govAuthPassword='govadminpassword',
    providerPostgresPassword='postgres',
    providerAuthUser='provider',
    providerAuthPassword='providerpassword',
    bankPostgresPassword='postgres',
    bankAuthUser='bankadmin',
    bankAuthPassword='bankadminpassword',    
  ):: {
    namespace: namespace.new(tk.env.spec.namespace),

    // Government
    'postgres.gov': postgres.new(
      image=$._config.postgres.image,
      org='gov',
      password=govPostgresPassword,
      databases=['domain', 'government_ledger', 'government_json'],
    ),
    domain: domain.new($._config.canton.image),
    'ledger.gov': {},  // << contract-builder
    'json.gov': {},
    'auth.gov': auth.new(
      image=$._config.authServer.image,
      org='gov',
      user=govAuthUser,
      password=govAuthPassword,
    ),
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
    'auth.provider': auth.new(
      image=$._config.authServer.image,
      org='provider',
      user=providerAuthUser,
      password=providerAuthPassword,
    ),
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
    'auth.bank': auth.new(
      image=$._config.authServer.image,
      org='bank',
      user=bankAuthUser,
      password=bankAuthPassword,
    ),
    'gateway.bank': {},

    // depends on all services above; can be done using k8s?
    'create-factory': {},
  },
}
+ (import 'config.libsonnet')

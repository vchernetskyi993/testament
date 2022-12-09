local commons = import 'commons.libsonnet';
local auth = import 'components/auth.libsonnet';
local domain = import 'components/domain.libsonnet';
local json = import 'components/json.libsonnet';
local ledger = import 'components/ledger.libsonnet';
local postgres = import 'components/postgres.libsonnet';
local k = import 'k.libsonnet';
local tk = import 'tk';

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
    cantonConfigMap: commons.canton.configMap,

    // Government
    'postgres.gov': postgres.new(
      image=$._config.postgres.image,
      org='gov',
      password=govPostgresPassword,
      databases=['domain', 'government_ledger', 'government_json'],
    ),
    domain: domain.new($._config.canton.image),
    'ledger.gov': ledger.new(
      image=$._config.ledger.image,
      org='gov',
      participant='government',
    ),
    'json.gov': json.new(
      image=$._config.json.image,
      org='gov',
      participant='government'
    ),
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
    'ledger.provider': ledger.new(
      image=$._config.ledger.image,
      org='provider',
      participant='provider',
    ),
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
    'ledger.bank': ledger.new(
      image=$._config.ledger.image,
      org='bank',
      participant='bank',
    ),
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

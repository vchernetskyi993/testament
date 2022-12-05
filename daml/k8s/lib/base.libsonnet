local commons = import 'commons.libsonnet';
local k = import 'k.libsonnet';
local postgres = import 'postgres.libsonnet';
local tk = import 'tk';
local domainConf = importstr 'configs/domain.conf';

local namespace = k.core.v1.namespace;
local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local envVar = k.core.v1.envVar;
local configMap = k.core.v1.configMap;
local volumeMount = k.core.v1.volumeMount;
local volume = k.core.v1.volume;

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
    domain: {
      config: configMap.new('domain-config', {
        'domain.conf': domainConf,
      }),
      deployment:
        deployment.new('domain', replicas=3, containers=[
          container.new('domain', $._config.canton.image)
          + container.withCommand([
            'bin/canton',
            'daemon',
            '-c',
            '/configs/domain.conf',
          ])
          + container.withEnv([
            envVar.new('POSTGRES_HOST', '$(POSTGRES_GOV_SERVICE_HOST)'),
            envVar.new('POSTGRES_PORT', '$(POSTGRES_GOV_SERVICE_PORT)'),
            envVar.new('POSTGRES_USER', 'domain'),
            envVar.new('POSTGRES_DB', 'domain'),
            envVar.new('POSTGRES_PASSWORD', 'domain'),
          ])
          + container.withVolumeMounts([
            volumeMount.new('domain-configs-volume', '/configs'),
          ])
          + container.livenessProbe.httpGet.withPath('/health')
          + container.livenessProbe.httpGet.withPort(7000)
          + container.livenessProbe.withInitialDelaySeconds(10)
          + container.livenessProbe.withPeriodSeconds(3),
        ])
        + deployment.spec.template.spec.withVolumes([
          volume.fromConfigMap('domain-configs-volume', 'domain-config'),
        ]),
      service: commons.service.new(self.deployment, [
        { name: 'public', port: 5000 },
        { name: 'admin', port: 5001 },
      ]),
    },
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

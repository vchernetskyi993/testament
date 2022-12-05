local k = import 'k.libsonnet';
local util = import 'ksonnet-util/util.libsonnet';
local tk = import 'tk';

local namespace = k.core.v1.namespace;
local secret = k.core.v1.secret;
local pv = k.core.v1.persistentVolume;
local pvc = k.core.v1.persistentVolumeClaim;
local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local volumeMount = k.core.v1.volumeMount;
local volume = k.core.v1.volume;
local secretRef = k.core.v1.envFromSource.secretRef;
local service = k.core.v1.service;
local servicePort = k.core.v1.servicePort;
local configMap = k.core.v1.configMap;

local accessMode = 'ReadWriteMany';
local storage = { storage: '1Gi' };

local createDb(name) = |||
  CREATE USER %(db)s WITH ENCRYPTED PASSWORD '%(db)s';
  CREATE DATABASE %(db)s;
  GRANT ALL ON DATABASE %(db)s TO %(db)s;
||| % { db: name };

{
  platform(
    postgresGovPassword='postgres',
  ):: {
    // namespace: namespace.new(tk.env.spec.namespace),
    namespace: namespace.new('daml-testament'),

    // Government
    'postgres.gov': {
      secret:
        secret.new('postgres-secret', {
          POSTGRES_PASSWORD: std.base64(postgresGovPassword),
        }),
      volume:
        pv.new('postgres-gov-pv')
        + pv.spec.withAccessModes(accessMode)
        + pv.spec.withCapacity(storage)
        + pv.spec.hostPath.withPath('/data/gov/'),
      volumeClaim:
        pvc.new('postgres-gov-pv-claim')
        + pvc.spec.withAccessModes(accessMode)
        + pvc.spec.resources.withRequests(storage),
      initConfig:
        configMap.new('init-gov-db-config', {
          'init-db.sql': std.join('\n', [
            createDb('domain'),
            createDb('government_ledger'),
            createDb('government_json'),
          ]),
        }),
      deployment:
        deployment.new('postgres-gov', containers=[
          container.new('postgres-gov', $._config.postgres.image)
          + container.livenessProbe.exec.withCommand([
            'psql',
            '-U',
            'postgres',
            '-c',
            'SELECT 1',
          ])
          + container.livenessProbe.withInitialDelaySeconds(45)
          + container.livenessProbe.withTimeoutSeconds(2)
          + { readinessProbe: self.livenessProbe }
          + container.withVolumeMounts([
            volumeMount.new('postgres-gov-data', '/var/lib/postgresql/data'),
            volumeMount.new('postgres-gov-init-data', '/docker-entrypoint-initdb.d'),
          ])
          + container.withEnvFrom(secretRef.withName('postgres-secret')),
        ])
        + deployment.spec.template.spec.withVolumes([
          volume.fromPersistentVolumeClaim('postgres-gov-data', 'postgres-gov-pv-claim'),
          volume.fromConfigMap('postgres-gov-init-data', 'init-gov-db-config'),
        ]),
      service:
        util.serviceFor(self.deployment)
        + service.spec.withPorts(servicePort.new(5432, 5432))
        + service.spec.withType('NodePort'),
    },
    domain: {},
    'ledger.gov': {},  // << contract-builder
    'json.gov': {},
    'auth.gov': {},
    'nginx.gov': {},  // << ui-builder

    // Provider
    'postgres.provider': {},  // << prepare-sql.provider
    'ledger.provider': {},  // << contract-builder
    'json.provider': {},
    'auth.provider': {},
    'gateway.provider': {},

    // Bank
    'postgres.bank': {},  // << prepare-sql.bank
    'ledger.bank': {},  // << contract-builder
    'json.bank': {},
    'auth.bank': {},
    'gateway.bank': {},

    // depends on all services above; can be done using k8s?
    'create-factory': {},
  },
}
+ (import 'config.libsonnet')

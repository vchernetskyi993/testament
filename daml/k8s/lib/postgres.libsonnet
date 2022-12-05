local commons = import 'commons.libsonnet';
local k = import 'k.libsonnet';

local secret = k.core.v1.secret;
local pv = k.core.v1.persistentVolume;
local pvc = k.core.v1.persistentVolumeClaim;
local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local volumeMount = k.core.v1.volumeMount;
local volume = k.core.v1.volume;
local secretRef = k.core.v1.envFromSource.secretRef;
local configMap = k.core.v1.configMap;

local accessMode = 'ReadWriteMany';
local storage = { storage: '1Gi' };

local createDb(name) = |||
  CREATE USER %(db)s WITH ENCRYPTED PASSWORD '%(db)s';
  CREATE DATABASE %(db)s;
  GRANT ALL ON DATABASE %(db)s TO %(db)s;
||| % { db: name };

{
  new(image, org, password, databases):: {
    local secretName = 'postgres-%s-secret' % org,
    local pvName = 'postgres-%s-pv' % org,
    local pvcName = 'postgres-%s-pv-claim' % org,
    local configName = 'init-%s-db-config' % org,
    local name = 'postgres-%s' % org,
    local dataName = 'postgres-%s-data' % org,
    local initDataName = 'postgres-%s-init-data' % org,

    secret:
      secret.new(secretName, {
        POSTGRES_PASSWORD: std.base64(password),
      }),
    volume:
      pv.new(pvName)
      + pv.spec.withAccessModes(accessMode)
      + pv.spec.withCapacity(storage)
      + pv.spec.hostPath.withPath('/data/%s/' % org),
    volumeClaim:
      pvc.new(pvcName)
      + pvc.spec.withAccessModes(accessMode)
      + pvc.spec.resources.withRequests(storage),
    initConfig:
      configMap.new(configName, {
        'init-db.sql': std.join('\n', std.map(createDb, databases)),
      }),
    deployment:
      deployment.new(name, containers=[
        container.new(name, image)
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
          volumeMount.new(dataName, '/var/lib/postgresql/data'),
          volumeMount.new(initDataName, '/docker-entrypoint-initdb.d'),
        ])
        + container.withEnvFrom(secretRef.withName(secretName)),
      ])
      + deployment.spec.template.spec.withVolumes([
        volume.fromPersistentVolumeClaim(dataName, pvcName),
        volume.fromConfigMap(initDataName, configName),
      ]),
    service: commons.service.new(self.deployment, 5432),
  },
}

local k = import 'k.libsonnet';
local domainConf = importstr 'configs/domain.conf';
local commons = import 'commons.libsonnet';

local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local envVar = k.core.v1.envVar;
local configMap = k.core.v1.configMap;
local volumeMount = k.core.v1.volumeMount;
local volume = k.core.v1.volume;
local volumeProjection = k.core.v1.volumeProjection;

{
  new(image):: {
    local appName = 'domain',
    local configName = 'domain-config',
    local configsVolumeName = 'domain-configs-volume',

    config: configMap.new(configName, {
      'domain.conf': domainConf,
    }),
    deployment:
      deployment.new(appName, replicas=3, containers=[
        container.new(appName, image)
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
          volumeMount.new(configsVolumeName, '/configs'),
        ])
        + commons.canton.nodeHealth,
      ])
      + deployment.spec.template.spec.withVolumes([
        volume.withName(configsVolumeName)
        + volume.projected.withSources([
          volumeProjection.configMap.withName(configName),
          volumeProjection.configMap.withName(commons.canton.configMapName),
        ]),
      ]),
    service: commons.service.new(self.deployment, [
      { name: 'public', port: 5000 },
      { name: 'admin', port: 5001 },
    ]),
  },
}

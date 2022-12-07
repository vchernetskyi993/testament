local k = import 'k.libsonnet';
local domainConf = importstr 'configs/domain.conf';
local commons = import 'commons.libsonnet';

local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local envVar = k.core.v1.envVar;
local configMap = k.core.v1.configMap;
local volumeMount = k.core.v1.volumeMount;
local volume = k.core.v1.volume;

{
  new(image):: {
    config: configMap.new('domain-config', {
      'domain.conf': domainConf,
    }),
    deployment:
      deployment.new('domain', replicas=3, containers=[
        container.new('domain', image)
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
}

local commons = import 'commons.libsonnet';
local auth = import 'components/auth.libsonnet';
local k = import 'k.libsonnet';

local ledgerConf = importstr 'configs/ledger-node.conf';
local featuresConf = importstr 'configs/features.conf';
local participantConf = importstr 'configs/participant.conf';

local participantScript = importstr 'scripts/participant.scala';

local configMap = k.core.v1.configMap;
local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local envVar = k.core.v1.envVar;
local volumeMount = k.core.v1.volumeMount;
local volume = k.core.v1.volume;
local volumeProjection = k.core.v1.volumeProjection;

{
  new(image, org, participant):: {
    local configName = 'ledger-%s-config' % org,
    local configsVolumeName = 'ledger-%s-config-volume' % org,
    local appName = 'ledger-%s' % org,
    local scriptsName = 'ledger-%s-scripts' % org,
    local scriptsVolumeName = 'ledger-%s-scripts' % org,

    config: configMap.new(configName, {
      'participant.conf': participantConf % { participant: participant },
      'ledger-node.conf': ledgerConf,
      'features.conf': featuresConf,
    }),
    scripts: configMap.new(scriptsName, {
      'participant.scala': participantScript,
    }),
    deployment:
      deployment.new(appName, containers=[
        container.new(appName, image)
        + container.withImagePullPolicy('Never')
        + container.withEnv([
          envVar.new('POSTGRES_HOST', '$(POSTGRES_%s_SERVICE_HOST)' % std.asciiUpper(org)),
          envVar.new('POSTGRES_PORT', '$(POSTGRES_%s_SERVICE_PORT)' % std.asciiUpper(org)),
          envVar.new('POSTGRES_USER', '%s_ledger' % participant),
          envVar.new('POSTGRES_DB', '%s_ledger' % participant),
          envVar.new('POSTGRES_PASSWORD', '%s_ledger' % participant),
          envVar.new('DOMAIN_URL', 'http://$(DOMAIN_SERVICE_HOST):$(DOMAIN_SERVICE_PORT_PUBLIC)'),
          envVar.new('PARTICIPANT_NAME', participant),
          envVar.fromSecretRef('PARTICIPANT_USER', auth.secretName(org), 'PARTICIPANT_USER'),
          envVar.new(
            'JWKS_URL',
            'http://$(AUTH_%(org)s_SERVICE_HOST):$(AUTH_%(org)s_SERVICE_PORT)'
            % { org: std.asciiUpper(org) }
          ),
        ])
        + container.withVolumeMounts([
          volumeMount.new(configsVolumeName, '/configs'),
          volumeMount.new(scriptsVolumeName, '/scripts'),
        ])
        + commons.canton.nodeHealth,
      ])
      + deployment.spec.template.spec.withVolumes([
        volume.withName(configsVolumeName)
        + volume.projected.withSources([
          volumeProjection.configMap.withName(configName),
          volumeProjection.configMap.withName(commons.canton.configMapName),
        ]),
        volume.fromConfigMap(scriptsVolumeName, scriptsName),
      ]),
    service: commons.service.new(self.deployment, [
      { name: 'public', port: 6000 },
      { name: 'admin', port: 6001 },
    ]),
  },
}

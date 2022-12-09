local commons = import 'commons.libsonnet';
local k = import 'k.libsonnet';

local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local envVar = k.core.v1.envVar;

{
  new(image, org, participant):: {
    local appName = 'json-%s' % org,
    local port = 7575,

    deployment: deployment.new(appName, containers=[
      container.new(appName, image)
      + container.withEnv([
        envVar.new(
          'DB_URL',
          'jdbc:postgresql://$(POSTGRES_%(org)s_SERVICE_HOST):$(POSTGRES_%(org)s_SERVICE_PORT)/%(participant)s_json'
          % { org: std.asciiUpper(org), participant: participant }
        ),
        envVar.new('DB_USER', '%s_json' % participant),
        envVar.new('DB_PASSWORD', '%s_json' % participant),
        envVar.new('LEDGER_HOST', '$(LEDGER_%(org)s_SERVICE_HOST)' % std.asciiUpper(org)),
        envVar.new('LEDGER_PORT', '$(LEDGER_%(org)s_SERVICE_PORT_PUBLIC)' % std.asciiUpper(org)),
      ])
      + container.withImagePullPolicy('Never')
      + container.livenessProbe.httpGet.withPath('/livez')
      + container.livenessProbe.httpGet.withPort(port)
      // + container.readinessProbe.httpGet.withPath('/readyz')
      // + container.readinessProbe.httpGet.withPort(port),
    ]),
    service: commons.service.new(self.deployment, port),
  },
}

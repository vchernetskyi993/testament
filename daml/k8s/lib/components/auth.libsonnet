local commons = import 'commons.libsonnet';
local k = import 'k.libsonnet';

local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local envVar = k.core.v1.envVar;
local secret = k.core.v1.secret;
local secretRef = k.core.v1.envFromSource.secretRef;

{
  new(image, org, user, password):: {
    local port = 8080,
    local secretName = 'auth-%s-secret' % org,
    local appName = 'auth-%s' % org,

    secret:
      secret.new(secretName, {
        PARTICIPANT_USER: std.base64(user),
        PARTICIPANT_PASSWORD: std.base64(password),
      }),
    deployment: deployment.new(appName, containers=[
      container.new(appName, image)
      + container.withEnvFrom(secretRef.withName(secretName))
      + container.withImagePullPolicy('Never')
      + container.livenessProbe.httpGet.withPath('/healthz')
      + container.livenessProbe.httpGet.withPort(port)
      + container.readinessProbe.httpGet.withPath('/readyz')
      + container.readinessProbe.httpGet.withPort(port),
    ]),
    service: commons.service.new(self.deployment, port),
  },
}

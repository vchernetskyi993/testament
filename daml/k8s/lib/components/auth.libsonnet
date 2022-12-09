local commons = import 'commons.libsonnet';
local k = import 'k.libsonnet';

local deployment = k.apps.v1.deployment;
local container = k.core.v1.container;
local secret = k.core.v1.secret;
local secretRef = k.core.v1.envFromSource.secretRef;

{
  new(image, org, user, password):: {
    local port = 8080,
    local appName = 'auth-%s' % org,

    secret:
      secret.new($.secretName(org), {
        PARTICIPANT_USER: std.base64(user),
        PARTICIPANT_PASSWORD: std.base64(password),
      }),
    deployment: deployment.new(appName, containers=[
      container.new(appName, image)
      + container.withEnvFrom(secretRef.withName($.secretName(org)))
      + container.withImagePullPolicy('Never')
      + container.livenessProbe.httpGet.withPath('/healthz')
      + container.livenessProbe.httpGet.withPort(port)
      + container.readinessProbe.httpGet.withPath('/readyz')
      + container.readinessProbe.httpGet.withPort(port),
    ]),
    service: commons.service.new(self.deployment, port),
  },
  secretName(org):: 'auth-%s-secret' % org,
}

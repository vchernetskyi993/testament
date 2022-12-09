local k = import 'k.libsonnet';
local util = import 'ksonnet-util/util.libsonnet';
local storageConf = importstr 'configs/storage.conf';
local healthConf = importstr 'configs/health.conf';

local service = k.core.v1.service;
local servicePort = k.core.v1.servicePort;
local configMap = k.core.v1.configMap;
local container = k.core.v1.container;

{
  service: {
    new(deployment, port)::
      util.serviceFor(deployment)
      + service.spec.withPorts(
        if std.isArray(port)
        then std.map(
          function(p) servicePort.newNamed(p.name, p.port, p.port),
          port
        )
        else servicePort.new(port, port)
      )
      + service.spec.withType('NodePort'),
  },
  canton: {
    configMapName:: 'canton-common-configs',
    configMap: configMap.new($.canton.configMapName, {
      'storage.conf': storageConf,
      'health.conf': healthConf,
    }),
    nodeHealth::
      container.livenessProbe.httpGet.withPath('/health')
      + container.livenessProbe.httpGet.withPort(7000)
      + container.livenessProbe.withInitialDelaySeconds(30)
      + container.livenessProbe.withPeriodSeconds(5),
  },
}

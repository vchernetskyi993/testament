local k = import 'k.libsonnet';
local util = import 'ksonnet-util/util.libsonnet';

local service = k.core.v1.service;
local servicePort = k.core.v1.servicePort;

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
}

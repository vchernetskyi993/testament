local k = import 'k.libsonnet';
local util = import 'ksonnet-util/util.libsonnet';

local service = k.core.v1.service;
local servicePort = k.core.v1.servicePort;

{
  service: {
    new(deployment, port)::
      util.serviceFor(deployment)
      + service.spec.withPorts(servicePort.new(port, port))
      + service.spec.withType('NodePort'),
  },
}

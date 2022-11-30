local k = import 'k.libsonnet';
local tk = import 'tk';

local namespace = k.core.v1.namespace;

{
  namespace: namespace.new(tk.env.spec.namespace),

  // Government
  'postgres.gov': {
    volume: {},
    volumeClaim: {},
    deployment: {},
    service: {},
  }, // << prepare-sql.gov
  domain: {},
  'ledger.gov': {}, // << contract-builder
  'json.gov': {},
  'auth.gov': {},
  'nginx.gov': {}, // << ui-builder

  // Provider
  'postgres.provider': {}, // << prepare-sql.provider
  'ledger.provider': {}, // << contract-builder
  'json.provider': {},
  'auth.provider': {},
  'gateway.provider': {},

  // Bank
  'postgres.bank': {}, // << prepare-sql.bank
  'ledger.bank': {}, // << contract-builder
  'json.bank': {},
  'auth.bank': {},
  'gateway.bank': {},

  // depends on all services above; can be done using k8s?
  'create-factory': {}
}

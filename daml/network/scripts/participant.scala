
val participant = participants.local.head

// connect domain
participant.domains.connect("testament", "http://domain:5000")

utils.retry_until_true {
  participant.domains.active("testament")
}

// create party
val party = participant.parties.enable(sys.props.get("party.name").get)

// create default user
participant.ledger_api.users.create(
  id = sys.props.get("user").get,
  actAs = Set(party.toLf),
  primaryParty = Some(party.toLf),
  readAs = Set(party.toLf),
)

// upload contracts
government.dars.upload(sys.props.get("dar.path").get)

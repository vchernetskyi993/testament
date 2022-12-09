
val participant = participants.local.head

if (participant.domains.list_registered().isEmpty) {
  // connect domain
  participant.domains.connect("testament", sys.props.get("domain.url").get)
  
  utils.retry_until_true {
    participant.domains.active("testament")
  }
  
  // create party
  val party = participant.parties.list(sys.props.get("participant.name").get).head.party
  
  // create default user
  participant.ledger_api.users.create(
    id = sys.props.get("participant.user").get,
    actAs = Set(party.toLf),
    primaryParty = Some(party.toLf),
    readAs = Set(party.toLf),
    participantAdmin = true,
  )
  
  // upload contracts
  participant.dars.upload(sys.props.get("dar.path").get)
}

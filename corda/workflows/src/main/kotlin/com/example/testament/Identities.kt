package com.example.testament

import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService

const val BANK_ORG = "Bank"
const val PROVIDER_ORG = "TestamentProvider"

fun IdentityService.government(): Party =
    partyFromName(CordaX500Name.parse("C=US, L=New York, O=Government"))!!

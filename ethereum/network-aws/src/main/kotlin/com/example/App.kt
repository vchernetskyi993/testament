package com.example

import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.managedblockchain.CfnNode
import software.amazon.awscdk.services.managedblockchain.CfnNode.NodeConfigurationProperty

fun main() {
    val app = App()

    val stack = Stack.Builder.create(app, "EthereumStack").build()

    CfnNode.Builder.create(stack, "EthereumNode")
        .networkId("n-ethereum-goerli")
        .nodeConfiguration(
            NodeConfigurationProperty.builder()
                .availabilityZone("us-east-1a")
                .instanceType("bc.t3.large")
                .build()
        )
        .build()

    app.synth()
}

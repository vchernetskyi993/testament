# Voting platform network

Uses AWS CDK to deploy Ethereum node to Goerli testnet.

## Deployment

The `cdk.json` file tells the CDK Toolkit how to execute your app.

- `cdk diff` - compare deployed stack with current state
- `cdk deploy [--hotswap]` - deploy this stack to your default AWS account/region
- `cdk destroy` - delete this stack

To retrieve Ethereum node endpoints:

```bash
# list available nodes and manually find your node id
aws managedblockchain list-nodes --network-id n-ethereum-goerli

# get node endpoints by node id
aws managedblockchain get-node \
    --node-id nd-O6VOPJKK4JFB5CVBSH2RUIVAIA \
    --network-id n-ethereum-goerli | jq ".Node.FrameworkAttributes.Ethereum"
```

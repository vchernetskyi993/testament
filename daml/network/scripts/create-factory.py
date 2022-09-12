from functools import reduce
import json
from pathlib import Path
from time import sleep
from typing import Dict, Optional, TypedDict
import requests
import typer
import fastjsonschema

PARTIES = ["government", "bank", "provider"]
OUTPUT_SCHEMA = fastjsonschema.compile(
    {
        "type": "object",
        "properties": {
            "contractId": {"type": "string", "minLength": 1},
            "parties": {
                "type": "object",
                "properties": {
                    party: {"type": "string", "minLength": 1} for party in PARTIES
                },
                "required": PARTIES,
            },
        },
        "required": ["contractId", "parties"],
    }
)

class LoginData(TypedDict):
    user: str
    password: str


def main(
    output: str = typer.Option(...),
    provider_user: str = typer.Option(...),
    provider_password: str = typer.Option(...),
    gov_user: str = typer.Option(...),
    gov_password: str = typer.Option(...),
    bank_user: str = typer.Option(...),
    bank_password: str = typer.Option(...),
): 
    bank_jwt = retrieve_token("auth.bank", bank_user, bank_password)
    if is_valid(output, bank_jwt):
        print("Factory config is already valid. Exiting.")
        return

    provider_jwt = retrieve_token("auth.provider", provider_user, provider_password)
    party_ids = fetch_party_ids(provider_jwt)

    print(f"Factory proposal: {party_ids}")
    proposal_id = propose_factory(party_ids, provider_jwt)

    government_jwt = retrieve_token("auth.gov", gov_user, gov_password)

    print(f"Creating factory for proposal: {proposal_id}")
    factory_id = create_factory(proposal_id, government_jwt)

    with open(output, "w") as result:
        json.dump({"contractId": factory_id, "parties": party_ids}, result)


def is_valid(output: str, jwt: str) -> bool:
    path = Path(output)
    if not path.exists():
        print("Factory config not present. Generating...")
        return False

    with path.open() as file:
        result_json = json.load(file)
        try:
            OUTPUT_SCHEMA(result_json)
        except fastjsonschema.JsonSchemaValueException:
            print("Existing factory config is invalid. Generating new one...")
            return False

        factory = fetch_factory(result_json["contractId"], jwt)
        if factory is None:
            print("Factory from existing config not present. Creating new one...")
            return False

    return True


def fetch_party_ids(jwt: str) -> Dict[str, str]:
    return reduce(
        lambda res, i: res | {next(p for p in PARTIES if p in i): i},
        map(
            lambda party: party["identifier"],
            fetch_parties(jwt),
        ),
        {},
    )


def fetch_parties(jwt: str) -> dict:
    retries = 20
    attempt = 0
    while attempt != retries:
        try:
            result = requests.get(
                url="http://json.provider:7575/v1/parties",
                headers={"Authorization": f"Bearer {jwt}"},
            ).json()["result"]
            if len(result) == len(PARTIES):
                return result
            attempt = wait("Waiting for all parties to be ready...", attempt)
        except (requests.ConnectionError, KeyError):
            attempt = wait("Waiting for json.provider to be ready...", attempt)
    raise Exception("Ceased waiting for parties...")


def wait(message: str, attempt) -> int:
    print(message)
    sleep(3)
    return attempt + 1


def propose_factory(party_ids: Dict[str, str], jwt: str) -> str:
    return json_api_call(
        "http://json.provider:7575/v1/create",
        jwt,
        {"templateId": "Main.Factory:NewFactoryProposal", "payload": party_ids},
    )["contractId"]


def create_factory(proposal_id: str, jwt: str) -> str:
    return next(
        map(
            lambda e: e["created"],
            filter(
                lambda e: "created" in e,
                json_api_call(
                    "http://json.gov:7575/v1/exercise",
                    jwt,
                    {
                        "templateId": "Main.Factory:NewFactoryProposal",
                        "contractId": proposal_id,
                        "choice": "CreateFactory",
                        "argument": {},
                    },
                )["events"],
            ),
        )
    )["contractId"]


def fetch_factory(contract_id: str, jwt: str) -> Optional[dict]:
    return json_api_call(
        "http://json.bank:7575/v1/fetch",
        jwt,
        {"contractId": contract_id},
    )


def json_api_call(url: str, jwt: str, body: dict) -> dict:
    retries = 20
    attempt = 0
    while attempt != retries:
        try:
            result = requests.post(
                url=url,
                headers={"Authorization": f"Bearer {jwt}"},
                json=body,
            ).json()
            if result["status"] == 200:
                return result["result"]
            raise requests.ConnectionError()
        except (requests.ConnectionError, KeyError):
            attempt = wait(f"Waiting for {url} to be ready...", attempt)
            
    raise Exception(f"JSON-API error: {json.dumps(result)}")


def retrieve_token(host: str, username: str, password: str) -> str:
    return requests.post(
        url=f"http://{host}:8080/authenticate",
        json={"user": username, "password": password},
    ).json()["token"]


if __name__ == "__main__":
    typer.run(main)

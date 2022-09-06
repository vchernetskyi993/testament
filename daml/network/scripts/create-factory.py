from dataclasses import dataclass
from functools import reduce
import json
from os import environ
from time import sleep
from typing import Dict
import requests

PARTIES = ["government", "bank", "provider"]


@dataclass
class FactoryConfig:
    provider_user: str
    provider_password: str
    gov_user: str
    gov_password: str


def create_factory(config: FactoryConfig) -> str:
    provider_jwt = retrieve_token(
        "auth.provider", config.provider_user, config.provider_password
    )
    government_jwt = retrieve_token("auth.gov", config.gov_user, config.gov_password)
    party_ids: Dict[str, str] = reduce(
        lambda res, i: res | {next(p for p in PARTIES if p in i): i},
        map(
            lambda party: party["identifier"],
            fetch_parties(provider_jwt),
        ),
        {},
    )
    print(json.dumps(party_ids))
    proposal_id = json_api_call(
        "http://json.provider:7575/v1/create",
        provider_jwt,
        {"templateId": "Main.Factory:NewFactoryProposal", "payload": party_ids},
    )["contractId"]
    return next(
        map(
            lambda e: e["created"],
            filter(
                lambda e: "created" in e,
                json_api_call(
                    "http://json.gov:7575/v1/exercise",
                    government_jwt,
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


def fetch_parties(jwt: str) -> dict:
    retries = 10
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


def json_api_call(url: str, jwt: str, body: dict) -> dict:
    retries = 10
    attempt = 0
    while attempt != retries:
        result = requests.post(
            url=url,
            headers={"Authorization": f"Bearer {jwt}"},
            json=body,
        ).json()
        if result["status"] == 200:
            return result["result"]
        attempt = wait("Waiting for json-api to be ready...", attempt)
    raise Exception(f"JSON-API error: {json.dumps(result)}")
    


def retrieve_token(host: str, username: str, password: str) -> str:
    return requests.post(
        url=f"http://{host}:8080/authenticate",
        json={"user": username, "password": password},
    ).json()["token"]


if __name__ == "__main__":
    factory_id = create_factory(
        FactoryConfig(
            provider_user=environ["PROVIDER_USER"],
            provider_password=environ["PROVIDER_PASSWORD"],
            gov_user=environ["GOV_USER"],
            gov_password=environ["GOV_PASSWORD"],
        )
    )
    with open(environ["RESULT_PATH"], "w") as result:
        json.dump({"contractId": factory_id}, result)

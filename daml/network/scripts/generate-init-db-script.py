from typing import List
from jinja2 import Environment, FileSystemLoader
import typer


def main(
    output: str = typer.Option(...),
    database: List[str] = typer.Option(...),
) -> None:
    jinja_env = Environment(loader=FileSystemLoader("./"))
    shell_script = jinja_env.get_template("init-db.sh.j2").render(
        statements=map(create_db_and_user_sql, database)
    )
    with open(output, "w") as out:
        out.write(shell_script)


def create_db_and_user_sql(database: str) -> str:
    return f"""
CREATE USER {database} WITH ENCRYPTED PASSWORD '{database}';
CREATE DATABASE {database};
GRANT ALL ON DATABASE {database} TO {database};
    """


if __name__ == "__main__":
    typer.run(main)

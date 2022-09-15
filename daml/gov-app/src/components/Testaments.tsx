import { useLedger } from "@daml/react";
import Button from "@mui/material/Button";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import React from "react";
import { TestamentData } from "../model";
import Title from "./Title";
import { Main } from "@daml.js/testament";
import { ContractId } from "@daml/types";
import { Typography } from "@mui/material";

export default function Testaments({
  testaments,
  pendingAccounts,
  activeAccounts,
}: {
  testaments: Map<string, TestamentData>;
  pendingAccounts: Map<string, ContractId<Main.Account.CreateAccount>>;
  activeAccounts: Map<string, number>;
}) {
  const ledger = useLedger();
  const factoryId = process.env.REACT_APP_FACTORY_ID;
  const announce = async (issuer: string) => {
    await ledger.exercise(
      Main.Factory.TestamentFactory.AnnounceExecution,
      factoryId as ContractId<Main.Factory.TestamentFactory>,
      { issuer }
    );
  };

  const confirmAccount = async (holder: string) => {
    await ledger.exercise(
      Main.Account.CreateAccount.SignAccountCreation,
      pendingAccounts.get(holder)!!,
      {}
    );
  };

  return (
    <React.Fragment>
      <Title>Testaments</Title>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>
              <Typography>Issuer</Typography>
            </TableCell>
            <TableCell width="25%">
              <Typography>Possession</Typography>
            </TableCell>
            <TableCell>
              <Typography>Inheritors</Typography>
            </TableCell>
            <TableCell>
              <Typography>Status</Typography>
            </TableCell>
            <TableCell align="right"></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {Array.from(testaments.values()).map((testament) => (
            <TableRow key={testament.issuer}>
              <TableCell>{testament.issuer}</TableCell>
              {/* TODO: set possession from accounts map */}
              <TableCell width="25%">
                {pendingAccounts.has(testament.issuer) ? (
                  <div>
                    Pending:
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => confirmAccount(testament.issuer)}
                      sx={{ ml: 1 }}
                    >
                      Confirm
                    </Button>
                  </div>
                ) : activeAccounts.has(testament.issuer) ? (
                  activeAccounts.get(testament.issuer)
                ) : (
                  "---"
                )}
              </TableCell>
              <TableCell>
                {Array.from(testament.inheritors.entries()).map(
                  ([id, share]) => (
                    <p key={id}>
                      {id}: {share}
                    </p>
                  )
                )}
              </TableCell>
              <TableCell>{testament.status}</TableCell>
              <TableCell align="right">
                {
                  <Button
                    variant="contained"
                    disabled={testament.status !== "Active"}
                    onClick={() => announce(testament.issuer)}
                  >
                    Announce
                  </Button>
                }
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </React.Fragment>
  );
}

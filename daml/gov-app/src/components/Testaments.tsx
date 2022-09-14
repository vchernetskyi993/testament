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

export default function Testaments({
  testaments,
}: {
  testaments: Map<string, TestamentData>;
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

  return (
    <React.Fragment>
      <Title>Testaments</Title>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Issuer</TableCell>
            <TableCell>Possession</TableCell>
            <TableCell>Inheritors</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right"></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {Array.from(testaments.values()).map((testament) => (
            <TableRow key={testament.issuer}>
              <TableCell>{testament.issuer}</TableCell>
              {/* TODO: set possession from accounts map */}
              <TableCell>---</TableCell>
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

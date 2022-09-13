import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import React from "react";
import Title from "./Title";

type TestamentData = {
  issuer: string;
  inheritors: { [id: string]: number };
  status: string;
};

const testaments: { [issuer: string]: TestamentData } = {
  "1": {
    issuer: "1",
    inheritors: {
      "2": 3000,
      "3": 7000,
    },
    status: "Active"
  },
};

const accounts: { [issuer: string]: TestamentData } = {
  "1": {
    issuer: "1",
    inheritors: {
      "2": 3000,
      "3": 7000,
    },
    announced: false,
    executed: false,
  },
};

export default function Orders() {
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
          {Object.values(testaments).map((testament) => (
            <TableRow key={testament.issuer}>
              <TableCell>{testament.issuer}</TableCell>
              <TableCell>Possession</TableCell>
              <TableCell>
                {Object.entries(testament.inheritors).map(([id, share]) => (
                  <p key={id}>
                    {id}: {share}
                  </p>
                ))}
              </TableCell>
              <TableCell>
                {testament.announced
                  ? "Announced"
                  : testament.executed
                  ? "Executed"
                  : "Active"}
              </TableCell>
              <TableCell align="right">{"Announce"}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Box sx={{ display: "flex", mt: 3, ml: 1 }}>
        <TextField margin="dense" label="Issuer" variant="standard" />
        <Button variant="contained" sx={{ mt: 3, mb: 2, ml: 1 }}>
          Watch
        </Button>
      </Box>
    </React.Fragment>
  );
}

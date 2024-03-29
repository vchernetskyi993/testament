import { createTheme, ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import Box from "@mui/material/Box";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Container from "@mui/material/Container";
import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Testaments from "./Testaments";
import { useStreamQueries } from "@daml/react";
import { ContractId } from "@daml/types";
import { Main } from "@daml.js/testament";
import { TestamentData } from "../model";
import React from "react";
import UserIcon from "./UserIcon";

const mdTheme = createTheme();

function DashboardContent({
  username,
  logout,
}: {
  username: string;
  logout: () => void;
}) {
  const testaments = useTestaments();
  const pendingAccounts = usePendingAccounts();
  const activeAccounts = useActiveAccounts();

  return (
    <ThemeProvider theme={mdTheme}>
      <Box sx={{ display: "flex" }}>
        <CssBaseline />
        <AppBar position="absolute">
          <Toolbar
            sx={{
              pr: "24px", // keep right padding when drawer closed
            }}
          >
            <Typography
              component="h1"
              variant="h6"
              color="inherit"
              noWrap
              sx={{ flexGrow: 1 }}
            >
              Dashboard
            </Typography>
            <UserIcon username={username} logout={logout} />
          </Toolbar>
        </AppBar>
        <Box
          component="main"
          sx={{
            backgroundColor: (theme) =>
              theme.palette.mode === "light"
                ? theme.palette.grey[100]
                : theme.palette.grey[900],
            flexGrow: 1,
            height: "100vh",
            overflow: "auto",
          }}
        >
          <Toolbar />
          <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <Paper sx={{ p: 2, display: "flex", flexDirection: "column" }}>
                  <Testaments
                    testaments={testaments}
                    pendingAccounts={pendingAccounts}
                    activeAccounts={activeAccounts}
                  />
                </Paper>
              </Grid>
            </Grid>
          </Container>
        </Box>
      </Box>
    </ThemeProvider>
  );
}

function useTestaments(): Map<string, TestamentData> {
  const stream = useStreamQueries(Main.Testament.Testament).contracts;
  return React.useMemo(
    () =>
      stream
        .map((event) => event.payload)
        .map((testament) => ({
          issuer: testament.issuer,
          inheritors: testament.inheritors
            .entriesArray()
            .reduce(
              (result, [i, s]) => result.set(i, +s),
              new Map<string, number>()
            ),
          status: testament.executed
            ? "Executed"
            : testament.announced
            ? "Announced"
            : "Active",
        }))
        .reduce(
          (result, testament) => result.set(testament.issuer, testament),
          new Map<string, TestamentData>()
        ),
    [stream]
  );
}

function usePendingAccounts(): Map<
  string,
  ContractId<Main.Account.CreateAccount>
> {
  const stream = useStreamQueries(Main.Account.CreateAccount).contracts;
  return React.useMemo(
    () =>
      new Map(stream.map((event) => [event.payload.holder, event.contractId])),
    [stream]
  );
}

function useActiveAccounts(): Map<string, number> {
  const stream = useStreamQueries(Main.Account.Account).contracts;
  return React.useMemo(
    () =>
      new Map(
        stream.map((event) => [event.payload.holder, +event.payload.possession])
      ),
    [stream]
  );
}

export default function Dashboard({
  username,
  logout,
}: {
  username: string;
  logout: () => void;
}) {
  return <DashboardContent username={username} logout={logout} />;
}

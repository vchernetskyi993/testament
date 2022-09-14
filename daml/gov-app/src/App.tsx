import React from "react";
import Dashboard from "./components/Dashboard";
import SignIn from "./components/SignIn";
import DamlLedger from "@daml/react";

function App() {
  const [token, setToken] = React.useState<string | undefined>();

  if (!token) {
    return <SignIn setToken={setToken} />;
  }

  const party =
    process.env.REACT_APP_DAML_PARTY ??
    error("REACT_APP_DAML_PARTY is required");
  return (
    <DamlLedger token={token} party={party}>
      <Dashboard />
    </DamlLedger>
  );
}

function error(message: string): never {
  throw Error(message);
}

export default App;

import React from "react";
import Dashboard from "./components/Dashboard";
import SignIn from "./components/SignIn";
import DamlLedger from "@daml/react";
import { UserData } from "./model";

function App() {
  const [user, setUser] = React.useState<UserData | null>(null);

  if (!user) {
    return <SignIn setUser={setUser} />;
  }

  const party =
    process.env.REACT_APP_DAML_PARTY ??
    error("REACT_APP_DAML_PARTY is required");
  return (
    <DamlLedger token={user.token} party={party}>
      <Dashboard
        username={user.username}
        logout={() => setUser(null)}
      />
    </DamlLedger>
  );
}

function error(message: string): never {
  throw Error(message);
}

export default App;

import React from "react";
import Dashboard from "./components/Dashboard";
import SignIn from "./components/SignIn";

function App() {
  const [token, setToken] = React.useState<string | undefined>()

  if (!token) {
    return <SignIn setToken={setToken} />
  }

  // TODO: wrap in DamlLedger
  return <Dashboard />;
}

export default App;

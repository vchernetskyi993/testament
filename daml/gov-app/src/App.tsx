import React from "react";
import Dashboard from "./components/Dashboard";
import SignIn from "./components/SignIn";
import DamlLedger from "@daml/react";
import { UserData } from "./model";
import decodeJwt, { JwtPayload } from "jwt-decode";

function App() {
  const [user, setUser] = React.useState<UserData | null>(null);
  const logout = () => setUser(null);
  React.useEffect(() => {
    if (!user) {
      return;
    }
    const timer = setTimeout(logout, getExpiration(user.token) - Date.now());
    return () => clearTimeout(timer);
  }, [user]);

  if (!user) {
    return <SignIn setUser={setUser} />;
  }

  const party =
    process.env.REACT_APP_DAML_PARTY ??
    error("REACT_APP_DAML_PARTY is required");
  return (
    <DamlLedger token={user.token} party={party}>
      <Dashboard username={user.username} logout={logout} />
    </DamlLedger>
  );
}

function error(message: string): never {
  throw Error(message);
}

function getExpiration(jwt: string): number {
  return decodeJwt<JwtPayload>(jwt).exp!! * 1000;
}

export default App;

import React from "react";
import SignIn from "./SignIn";

function App() {
  const [token, setToken] = React.useState<string | undefined>()

  if (!token) {
    return <SignIn setToken={setToken}></SignIn>
  }

  return (
    <div>
      <header>
        Hello World!
      </header>
    </div>
  );
}

export default App;

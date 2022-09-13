import axios from "axios";

export function login(user: string, password: string): Promise<string> {
  const url = process.env.REACT_APP_AUTH_SERVER_URL;
  return axios
    .post(`${url}/authenticate`, { user, password })
    .then((response) => response.data.token);
}

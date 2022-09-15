import axios from "axios";
import config from "./config";

const {
  auth: { baseUrl: url, username: user, password },
} = config;

export function jwt(): Promise<string> {
  return axios
    .post(`${url}/authenticate`, { user, password })
    .then((response) => response.data.token);
}

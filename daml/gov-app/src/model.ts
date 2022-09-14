
export type TestamentData = {
  issuer: string;
  inheritors: Map<string, number>;
  status: string;
};

export type UserData = {
  username: string;
  token: string;
};

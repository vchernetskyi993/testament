{
  _config:: {
    postgres: {
      image: 'postgres:14.6',
    },
    canton: {
      image: 'digitalasset/canton-open-source:2.3.3',
    },
    authServer: {
      image: 'testament/auth-server',
    },
  },
}

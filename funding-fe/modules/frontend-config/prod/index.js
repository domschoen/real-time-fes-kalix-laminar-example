module.exports = {
  config: {
    useAuthentication: true,
    websocketPort: 3500,
    defaultUser: "domschoen@example.com",
    msal: {
        authRedirectUrl: "https://chx-istprod-01t.hq.k.grp/customerrep",
        authority: "https://login.microsoftonline.com/e5733095-4425-4f08-b6ba-487b9a46a425",
        clientId: "169c85ac-80a2-4a35-b8d4-c1c215fe91ff"
    },
    kalix: {
      useCloud: true,
      cloudHostURL: "https://rapid-star-6893.us-east1.kalix.app",
      localHostURL: "http://localhost:9000"
    }
  },
  externals: {
    MsalModule : '@azure/msal-browser'
  }
}

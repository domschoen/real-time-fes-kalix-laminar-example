module.exports = {
  config: {
    useAuthentication: false,
    websocketPort: 3500,
    defaultUser: "domschoen@example.com",
    msal: {
        authRedirectUrl: "http://localhost:3000/",
        authority: "https://login.microsoftonline.com/adfadfadf",
        clientId: "asdfasdfa"
    },
    kalix: {
      useCloud: false,
      cloudHostURL: "https://bitter-mouse-7524.us-east1.kalix.app",
      localHostURL: "http://localhost:9000"
    }
  },
  externals: {
    MsalModule : '@azure/msal-browser'
  }
}


A full-stack todo app using basic hiccup for the views, compojure and ring for server-side routing and sessions, and datomic as a database. Containerized via Docker.

Getting started: 

  -- The uberjar for app should come prebuilt, but if you need to build it, run `lein uberjar` from within the `app/` directory.

  -- You'll need to add your own Datomic credentials. Inside `db/.credentials`, add your username and passkey for datomic. Also, you'll need to add your license key inside `db/config/dev-transactor.properties`.

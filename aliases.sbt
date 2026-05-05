addCommandAlias(
  "styleCheck",
  "scalafmtSbtCheck; scalafmtCheckAll; scalafixAll --check"
)
addCommandAlias(
  "styleFix",
  "scalafixAll; scalafmtSbt; scalafmtAll"
)
addCommandAlias(
  "dev",
  "tpolecatDevMode"
)
addCommandAlias(
  "ci",
  "tpolecatCiMode"
)
addCommandAlias(
  "unitTest",
  "libCommon/test; ctxCustomerApi/test; ctxCustomer/test; appServer/test"
)

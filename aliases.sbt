addCommandAlias(
  "styleCheck",
  "scalafmtSbtCheck; scalafmtCheckAll; scalafixAll --check"
)
addCommandAlias(
  "styleFix",
  "scalafixAll; scalafmtSbt; scalafmtAll"
)

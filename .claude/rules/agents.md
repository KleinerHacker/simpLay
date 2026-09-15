# Agents

* Long-running background activities MUST be delegated to an Agent, not run as a
  detached shell command
    * Applies to: full Gradle builds, `publishToMavenLocal` / publishing, doc
      generation, coverage reports, test runs and any other task that does not
      return near-instantly
    * The agent runs the task and reports the result back
* Short, near-instant commands (status queries, single-file reads, quick greps)
  stay inline

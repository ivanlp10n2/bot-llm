# llm-api

A simple integration between Telegram bots and Open Router API

## Run application

### prequisites:

```
BOT_KEY="telegram bot token after you create it"
OPEN_ROUTER_TOKEN="open router token after you sing up"
```

There are multiple ways to run it:

1) You can set the environment variables and run the application
```
BOT_KEY=xxxx OPEN_ROUTER_TOKEN=xxxx sbt run
```

2) You can create a `.env` file, with the following content, and then run the `(set -a && source '.env' && set +a && sbt run)` command.
```shell
BOT_KEY="xxxx"
OPEN_ROUTER_TOKEN="xxxx"
```

3) You can set the environment variables in your IDE as well

## Run tests

```shell
no test yet -> only deliver
```

## TODO list:

- ✅Secure sensible information
- Give feedback to user about processing
- Consider batching request to avoid: too many requests error
- Consider adding a cache layer
- Consider adding a retry mechanism
    - `java.net.SocketException: HTTP connection closed: https://openrouter.ai`
- Consider adding a logging mechanism
- Consider adding a metrics mechanism
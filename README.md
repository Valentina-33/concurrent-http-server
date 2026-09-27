# concurrent-http-server

Extension of **maintainable-application-server** — a small Java web framework built directly on
top of a raw `ServerSocket`, without any external framework (no Spring, no Netty). This repository
takes that course framework and adds the capabilities the *Containerizing and Deploying a Java Web
Application* workshop asks for beyond what it already had.

## 1. Current state of the framework

`maintainable-application-server` already let a developer register `GET` routes as lambdas, serve
static files, read query-string parameters, and configure the server through environment
variables (`PORT`, `APP_ENV`). Its one structural limitation was that `HttpServer` accepted and
handled connections **sequentially** — one client had to be fully served before the next `accept()`
call ran, so a slow request blocked every client behind it.

## 2. Changes introduced in this extension

| Requirement | Before | After |
|---|---|---|
| **Concurrent request handling** | `HttpServer` accepted one connection, handled it fully, then accepted the next. | The accept loop only accepts a socket and submits it to a fixed worker **thread pool** (`Executors.newFixedThreadPool`); multiple clients are served in parallel. Pool size is configurable via `THREAD_POOL_SIZE` (default 20). |
| **Graceful shutdown** | Stopping relied on the sequential loop naturally noticing a flag *after* the single in-flight request finished. | `stop()` closes the listening `ServerSocket` directly, which unblocks the accept loop immediately without accepting new work; the worker pool is then shut down with `awaitTermination`, letting every request **already in flight** finish (up to a 10s timeout) before the process exits. This matters specifically because the pool now runs work outside the accept loop. |
| **Environment-based port configuration** | Already present (`PORT`, default 8080). | Unchanged — kept, plus the new `THREAD_POOL_SIZE` variable follows the same pattern. |
| **Docker container execution** | Not present (previous deployment used systemd directly on the instance). | New `Dockerfile`, built on `amazoncorretto:21`, matching the pattern used in the Spring Boot workshop repository. |
| **AWS EC2 deployment** | Deployed previously via systemd (see `maintainable-application-server`'s own README). | Redeployed as a Docker container on EC2 — see [section 6](#6-aws-ec2-deployment). |

The routing, static-file serving, HTTP parsing/writing, and request/response wrapper classes are
carried over unchanged from `maintainable-application-server` — only `HttpServer` (the accept loop)
and `WebFramework` (pool-size configuration) changed.

### Architecture

```
edu.eci.arsw.app
    Application            → registers routes and static-files location
edu.eci.arsw.webframework
    WebFramework            → static facade: staticfiles(), get(), start(), stop()
    Router / RouteHandler   → maps a path to a lambda handler
    Request / Response      → thin wrappers the lambda receives
    HttpServer               → accepts connections, submits each to a worker thread pool,
                                shuts down gracefully
    StaticFileService        → serves a file from the configured static-files location
edu.eci.arsw.networking.http
    HttpRequest/HttpResponse/HttpRequestParser/HttpResponseWriter → low-level HTTP plumbing,
    reused unchanged from the networking lab this framework was built on top of
```

### A new route to demonstrate the concurrency change

`GET /slow` deliberately blocks for ~2 seconds before responding. Hitting it from several clients
at once and observing that they all finish in roughly the same ~2-second window (instead of
queueing up N × 2 seconds) is the simplest manual way to see the thread pool doing its job. The
automated test suite makes the same point without a stopwatch — see below.

## 3. Build and run locally

Requires JDK 17+ and Maven.

```bash
mvn clean package
java -jar target/concurrent-http-server-1.0.0.jar
```

The server starts on port `8080` by default. Configure it:

```bash
PORT=8081 THREAD_POOL_SIZE=50 GREETING_PREFIX=Hola java -jar target/concurrent-http-server-1.0.0.jar
```

Try the concurrency demo yourself — fire several requests at once and see them all return around
the same time instead of one after another:

```bash
for i in 1 2 3 4 5; do curl -s "http://localhost:8080/slow" & done; wait
```

Run the test suite:

```bash
mvn test
```

It includes `ConcurrencyTest`, which starts a real server, fires several requests at a blocking
route from independent client threads, and asserts the whole batch finishes in well under the time
sequential handling would require — an automated, repeatable proof that requests run in parallel,
not one that has to be eyeballed from timing output.

## 4. Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `PORT` | HTTP server port | `8080` |
| `THREAD_POOL_SIZE` | Size of the fixed worker-thread pool handling connections | `20` |
| `GREETING_PREFIX` | Prefix used by the `/hello` route | `Hello` |
| `APP_ENV` | `/shutdown` is only registered when this is `development` | `development` |

## 5. Docker

```bash
docker build -t pvlc/concurrent-http-server:1.0 .

docker run -d \
  --name concurrent-http-server \
  -e PORT=8080 \
  -e THREAD_POOL_SIZE=20 \
  -p 8081:8080 \
  pvlc/concurrent-http-server:1.0

curl "http://localhost:8081/hello?name=Docker"
```

`APP_ENV` defaults to `production` inside the image (set in the `Dockerfile`), so `/shutdown` is
disabled by default in containers — enable it only for local debugging with `-e APP_ENV=development`.

## 6. AWS EC2 deployment

Same approach as the workshop's Spring Boot repository: Amazon Linux 2023, Docker installed on the
instance, the published image pulled and run as a container.

```bash
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
# log out and reconnect for the group change to take effect

docker pull pvlc/concurrent-http-server:1.0

docker run -d \
  --name concurrent-http-server \
  --restart unless-stopped \
  -e PORT=8080 \
  -e THREAD_POOL_SIZE=20 \
  -p 8080:8080 \
  pvlc/concurrent-http-server:1.0
```

**Public deployment URL:** *pending — fill in once deployed:* `http://<ec2-public-dns>:8080/hello?name=AWS`

> 📌 **Pending evidence** — add once deployed:
> - `docs/ec2-deployment.png` — `docker ps` / `docker logs` on the instance.
> - `docs/ec2-endpoint.png` — the public URL responding from outside the instance.
> - `docs/concurrency-demo.png` (optional but recommended) — several `/slow` requests fired at once against the deployed instance, showing they complete together.

## 7. Evidence of progress

This repository's commit history documents the extension work directly — see
[the commit history on GitHub](https://github.com/Valentina-33/concurrent-http-server/commits/main),
in particular the commit implementing concurrent request handling and graceful shutdown in
`HttpServer`/`WebFramework`, plus the `ConcurrencyTest` that proves it.

| Requirement | Status |
|---|---|
| Concurrent request handling | ✅ Implemented — fixed worker thread pool, verified by `ConcurrencyTest` |
| Graceful server shutdown | ✅ Implemented — in-flight requests finish before the process exits |
| Environment-based port configuration | ✅ Carried over from the base framework (`PORT`) |
| Docker container execution | ✅ `Dockerfile` added, builds and runs locally |
| AWS EC2 deployment | ⏳ Pending — instance to be provisioned and evidence collected |

# PostgreSQL, pgvector, and pgAdmin 4 Setup

This guide explains how to start a local PostgreSQL database with the `pgvector` extension, manage it through pgAdmin 4 on Windows, and configure an application to connect to it. It uses the database name, user, and host port from the Docker command discussed with the team.

## 1. Understand the parts

- **PostgreSQL** is the database server. It stores regular application data and vector data.
- **pgvector** is a PostgreSQL extension that adds a `vector` column type and similarity-search operators. The extension files come with the selected Docker image, but it still needs to be enabled in each database where it is used.
- **pgAdmin 4** is a graphical client for connecting to PostgreSQL, browsing databases and tables, and running SQL. It is separate from the database server.
- **Docker** runs PostgreSQL in an isolated container, so this setup does not require installing the PostgreSQL server directly on Windows.

## 2. Prerequisites

1. Install and start [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).
2. Open PowerShell and confirm that Docker is running:

   ```powershell
   docker version
   ```

   The command should show both client and server information. If it only shows the client or reports that it cannot connect, start Docker Desktop and wait for its engine to become ready.

## 3. Start PostgreSQL with pgvector

Run this in PowerShell:

```powershell
docker run -d --name pgvector-db -e POSTGRES_DB=vectordb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 127.0.0.1:5433:5432 --mount source=pgvector_data,target=/var/lib/postgresql/data pgvector/pgvector:pg17
```

This uses the pgvector project's PostgreSQL 17 image. The earlier example used `ankane/pgvector`; if the team specifically needs that image, replace `pgvector/pgvector:pg17` at the end with `ankane/pgvector`.

| Part | What it does |
|---|---|
| `docker run` | Creates and starts a new container. |
| `-d` | Runs the container in the background (detached mode). |
| `--name pgvector-db` | Assigns the container a convenient name for later Docker commands. |
| `-e POSTGRES_DB=vectordb` | Initializes a database named `vectordb` the first time the data directory is initialized. |
| `-e POSTGRES_USER=postgres` | Initializes a PostgreSQL login role named `postgres`. |
| `-e POSTGRES_PASSWORD=postgres` | Sets that role's initial password. Use a unique password outside local development. |
| `-p 127.0.0.1:5433:5432` | Publishes container port `5432` on your computer's port `5433`, bound to localhost. Tools on this computer connect to `localhost:5433`. |
| `--mount source=pgvector_data,target=/var/lib/postgresql/data` | Stores database files in a named Docker volume so they remain when the container is stopped or removed. |
| `pgvector/pgvector:pg17` | Selects the image: PostgreSQL 17 with pgvector installed. |

The `127.0.0.1` binding is appropriate for a local developer database. If another computer needs to connect, plan and configure network access and credentials explicitly rather than exposing a database with the sample password.

### Confirm the container started

```powershell
docker ps
```

Look for `pgvector-db` in the list and a port mapping similar to `127.0.0.1:5433->5432/tcp`.

If it is not running, inspect its startup output:

```powershell
docker logs pgvector-db
```

## 4. Enable and verify the vector extension

The image contains pgvector, but PostgreSQL enables extensions per database. Run this once for `vectordb`:

```powershell
docker exec -it pgvector-db psql -U postgres -d vectordb -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

Then verify that PostgreSQL registered the extension:

```powershell
docker exec -it pgvector-db psql -U postgres -d vectordb -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
```

The query should return a version. `CREATE EXTENSION` must be run separately in every database where vector columns will be created.

## 5. Install pgAdmin 4 on Windows

1. Download the Windows installer from the [official pgAdmin 4 Windows page](https://www.pgadmin.org/download/pgadmin-4-windows/).
2. Run the installer and open pgAdmin 4.
3. If prompted to set a **master password**, create one and store it securely. This protects saved connection passwords in pgAdmin; it is **not** the PostgreSQL password.

## 6. Register the Docker database in pgAdmin

1. In pgAdmin's browser tree, right-click **Servers** and choose **Register → Server…**.
2. In the **General** tab, enter a display name, such as `Local pgvector`.
3. In the **Connection** tab, enter:

   | pgAdmin field | Value |
   |---|---|
   | Host name/address | `127.0.0.1` |
   | Port | `5433` |
   | Maintenance database | `vectordb` |
   | Username | `postgres` |
   | Password | The value assigned to `POSTGRES_PASSWORD` when the database was first initialized (`postgres` in the sample command). |

4. Optionally select **Save password?** if this is a trusted development computer.
5. Select **Save**. Expand the new server and its **Databases** node to browse the database.

These are the values used when pgAdmin is installed directly on Windows. If pgAdmin itself is run in a separate Docker container, `localhost` points to the pgAdmin container; the two containers must share a Docker network, and pgAdmin should use the PostgreSQL container/service name as its host instead.

### Try a SQL query in pgAdmin

Select `vectordb`, open **Tools → Query Tool**, and run:

```sql
SELECT extversion
FROM pg_extension
WHERE extname = 'vector';
```

You can also verify vector values and distance calculations:

```sql
SELECT '[1,2,3]'::vector <-> '[1,2,4]'::vector AS euclidean_distance;
```

## 7. Connect the application

From an application running directly on the same Windows computer, use:

```text
JDBC URL: jdbc:postgresql://localhost:5433/vectordb
Username: postgres
Password: postgres
```

The password must match the value used to initialize the container. For Spring Boot environment variables, the equivalent names are `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`, if the application uses Spring Boot's standard datasource configuration.

## 8. Start, stop, and inspect the database

```powershell
docker stop pgvector-db
docker start pgvector-db
docker logs -f pgvector-db
```

- `docker stop` stops PostgreSQL without deleting the container or its data.
- `docker start` starts the existing container again. Do not run the original `docker run` command again just to restart it; the name is already taken.
- `docker logs -f` follows PostgreSQL's logs. Press `Ctrl+C` to stop following logs; this does not stop the database.

The named volume `pgvector_data` keeps data separately from the container. Removing the container does not normally remove this named volume. Do not remove the volume if its database contents are needed.

## 9. Common problems

| Symptom | What to check |
|---|---|
| `docker: Cannot connect to the Docker daemon` | Start Docker Desktop and wait for the engine to report that it is running. |
| Port binding fails or pgAdmin cannot connect | Another process may already use host port `5433`. Change the left port in the mapping, for example `127.0.0.1:5434:5432`, then use `5434` in pgAdmin and the JDBC URL. |
| `password authentication failed` | Check the database password. The `POSTGRES_*` variables initialize a new data directory; changing those environment variables later does not change the password already stored in an existing volume. |
| `type "vector" does not exist` | Enable the extension in the database being used: `CREATE EXTENSION IF NOT EXISTS vector;`. Check that the selected image includes pgvector. |
| Database is empty after starting a different container | Make sure the same named volume, `pgvector_data`, is mounted at the PostgreSQL data directory. |

## References

- [pgvector project README and Docker image tags](https://github.com/pgvector/pgvector#docker)
- [pgvector getting-started SQL](https://github.com/pgvector/pgvector#getting-started)
- [pgAdmin 4 for Windows download](https://www.pgadmin.org/download/pgadmin-4-windows/)
- [pgAdmin server connection fields](https://www.pgadmin.org/docs/pgadmin4/latest/server_dialog.html)
- [Docker run and volume options](https://docs.docker.com/reference/cli/docker/container/run/)

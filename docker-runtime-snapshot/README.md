# Docker Runtime Snapshot

This directory contains the runtime artifacts copied from the currently running smart warehouse Docker containers on 2026-06-25 UTC.

It is an artifact snapshot, not a source-code recovery. The running images contain:

- `backend/app.jar`: Spring Boot backend artifact from `warehouse-backend`
- `frontend/`: nginx static frontend files from `warehouse-frontend`

Runtime container metadata:

| Service | Container | Image | Image digest | Created | Started |
| --- | --- | --- | --- | --- | --- |
| backend | `warehouse-backend` | `warehouse-system-backend` | `sha256:41a1fe73567973619f30092264b15f32206099b31000b61d341fbd60113ba57a` | `2026-06-23T12:32:42.887764994Z` | `2026-06-23T12:32:45.761054069Z` |
| frontend | `warehouse-frontend` | `warehouse-system-frontend` | `sha256:375d57e13f584eed57372b790bae9e6fab031b51261b6b8fcbb5f8cbba0681f3` | `2026-06-17T09:50:15.725078452Z` | `2026-06-17T09:50:17.195243244Z` |

Both containers were created by Docker Compose with:

- project: `warehouse-system`
- working directory: `/root/smart-warehouse-system`
- config file: `/root/smart-warehouse-system/docker-compose.yml`

Checksums for copied files are in `SHA256SUMS`.

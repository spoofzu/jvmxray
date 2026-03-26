# JVMXRay Changelog

All notable changes to this project are documented in this file.

---

## [Unreleased] - 2026-03-26

### Event Correlation Infrastructure

- **scope_chain field**: MCC now automatically maintains a `scope_chain` field (e.g., `HTTP>SQL>FileIO`) showing the nested sensor activation path. This enables "security stacktrace" reconstruction — seeing exactly how an attack propagated through the application.
- **parent_scope field**: Each event now includes `parent_scope` identifying the immediate parent sensor scope, enabling event tree reconstruction.
- **scope_depth field**: Integer nesting level for anomaly detection. Normal requests nest 2-3 deep; depth 6+ warrants investigation.
- **Spanning scopes for ProcessSensor**: ProcessInterceptor now uses proper enter/exit spanning scopes. Downstream sensors triggered by process execution (FileIO, Network) inherit the Process trace_id.
- **Spanning scopes for CryptoSensor**: All 5 crypto sub-interceptors (Cipher, CipherInit, MessageDigest, KeyStore, SSL) converted from degenerate to spanning scopes.
- **MCCScope added to 24 interceptors**: APICall, Authentication, ScriptEngine, Configuration (8 sub-interceptors), Reflection (6 sub-interceptors), Serialization (6 sub-interceptors), and DataTransfer sensors now participate in correlation. Events on background threads are no longer orphaned.

### Sensor Metadata Enrichment

- **APICallInterceptor** (previously captured almost nothing):
  - Now extracts: `request_uri`, `request_method`, `request_host`, `request_scheme`, `request_port`, `uses_tls`, `content_type`
  - Response metadata: `response_status`, `status_class` (success/redirect/client_error/server_error), `response_time_ms`
  - Error tracking: `error_class`, `error_message`

- **AuthenticationInterceptor** (previously a single generic interceptor):
  - Split into 6 operation-specific interceptors for precise auth tracking:
    - `SessionSetAttributeInterceptor` — tracks session attribute writes with hashed session_id
    - `SessionGetAttributeInterceptor` — tracks session attribute reads
    - `SessionInvalidateInterceptor` — tracks session destruction
    - `LoginInterceptor` — tracks JAAS login with success/failure and failure reason
    - `AuthenticateInterceptor` — tracks Spring Security authentication with principal extraction
    - `PrincipalInterceptor` — tracks principal name queries
  - All interceptors capture `auth_action`, `auth_mechanism`, and `session_id` (hashed for privacy)

- **ScriptEngineInterceptor** (previously captured only script_provided flag):
  - Now extracts: `engine_name`, `script_language` from ScriptEngine factory
  - Script analysis: `script_length`, `script_hash` (SHA-256), `script_snippet` (first 200 chars)
  - Security classification: `suspicious_patterns` (detects Runtime.exec, ProcessBuilder, Class.forName, java.net, URLClassLoader, etc.), `risk_level` (HIGH/LOW)
  - Distinguishes `script_eval` vs `engine_lookup` operations

- **ProcessInterceptor** enriched with: `execution_time_ms`, `status` (started/failed), `error_class`, `error_message`

### Database Schema

- **TRACE_ID column**: Added dedicated `TRACE_ID` column to `STAGE0_EVENT` table across all database implementations (MySQL, SQLite, Cassandra). Extracted from event keypairs during INSERT for efficient correlation queries.
- **TRACE_ID index**: Added `idx_stage0_trace_id` index for fast trace-based lookups. Replaces the need to scan KEYPAIRS JSON blobs.

### Bug Fixes

- **BindInterceptor**: Fixed duplicate status field assignment bug where the second `put("status", ...)` overwrote the first. The success value was incorrectly set to `"accepted"` instead of `"bound"`.
- **Socket interceptors**: Added MCCScope correlation to Accept, Bind, and Close interceptors (previously only Connect had it).
- **AcceptInterceptor**: Now captures `remote_address`, `remote_port`, `is_loopback`, `is_private_ip`.
- **BindInterceptor**: Now captures `local_port` and `backlog`.
- **CloseInterceptor**: Now captures `connection_duration_ms`.

### Naming Standardization

- **LibSensor**: Renamed `jarPath` to `jar_path` (snake_case consistency). Renamed `method` to `load_type` (clearer semantics — values are "static" or "dynamic").
- **DataTransferInterceptor**: Removed raw `@Advice.Origin` method field that had no analytical value.
- **APICallInterceptor**: Removed raw `method` field, replaced `operation=api_call` with `operation=http_client_send`.
- **ScriptEngineInterceptor**: Removed raw `method` field, replaced generic `operation=script_execution` with specific `script_eval` or `engine_lookup`.

### Not Yet Implemented

- **MCC-to-MDC Bridge** (Plan C): Bridging MCC correlation context into Logback's MDC for structured logging via patterns and SocketAppender. Deferred pending investigation of shaded MDC class resolution and recursive logging safeguards.
- **New sensor candidates**: ClassLoaderSensor, DNSSensor, JNDISensor, JMXSensor (identified in planning, not yet implemented).

---

## [0.0.1-alpha] - 2025-10-07

### Minor Fixes
- Sensor metadata improvements
- AI service development in progress
- OWASP Dependency Check integration for 3rd party libraries

## [0.0.1-alpha] - 2025-09-18

### Major Update
- AI-powered vulnerability analysis
- 15+ sensor types
- Multi-database support (SQLite/MySQL/Cassandra)
- MCP server for Claude Desktop integration
- Enhanced documentation suite
- Complete CI/CD pipeline

## [0.0.1-alpha] - 2025-04-23

### Platform Rearchitected
- Removed deprecated SecurityManager approach
- Moved to bytecode injection via ByteBuddy
- New sensor architecture

## [0.0.1-alpha] - 2025-02-20

### Improved Architecture
- Architecture documentation improvements
- Site docs forthcoming

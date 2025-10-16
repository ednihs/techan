# Techan

This service powers BTST (buy-today-sell-tomorrow) analytics by combining end-of-day bhavcopy data with intraday market feeds.

## Intraday Data Collection

Intraday minute bars are sourced from the configured FivePaisa market-feed API via `FivePaisaService`. The scheduler requests market snapshots for the previously shortlisted Day-1 BTST symbols every three minutes during market hours and persists them through `IntradayDataService`.

The service gracefully falls back to zeroed placeholders when the sandbox cannot reach the upstream API, so local development and tests can still execute without live market connectivity.

### FivePaisa Authentication

FivePaisa retired the legacy password-based `/authentication/login` endpoint. The service now follows the documented OAuth flow:

1. **Interactive login** – open `https://dev-openapi.5paisa.com/WebVendorLogin/VLogin/Index` with your `VendorKey` and callback URL. After entering the client code, PAN/PIN, and OTP, copy the `RequestToken` that FivePaisa appends to the callback URL.
2. **Configure the backend** – either set the `fivepaisa.api.request-token` property (environment variable or `application.yml`) or call `FivePaisaService.updateRequestToken(<token>)` at runtime.
3. **Token exchange** – on startup or whenever `authenticate()` is invoked, the service posts the token to `/GetAccessToken`, caches the returned bearer token, and automatically refreshes headers for subsequent market-feed and historical requests.

If no request token is configured, the service logs a warning and gracefully skips authentication so the rest of the application can continue to run in offline mode.

## Database Schema

Schema migrations are managed with Flyway (see `src/main/resources/db/migration`). The realtime pipeline introduces the `intraday_data` table to store the captured bars:

```sql
CREATE TABLE intraday_data (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(50) NOT NULL,
    trade_date DATE NOT NULL,
    trade_time TIME NOT NULL,
    open_price NUMERIC(12,2),
    high_price NUMERIC(12,2),
    low_price NUMERIC(12,2),
    close_price NUMERIC(12,2),
    volume BIGINT,
    bid NUMERIC(12,2),
    ask NUMERIC(12,2),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);
```

Flyway automatically applies this migration (`V6__create_intraday_data_table.sql`) alongside the existing schema scripts during application startup.

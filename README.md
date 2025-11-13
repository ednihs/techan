# Techan

This service powers BTST (buy-today-sell-tomorrow) analytics by combining end-of-day bhavcopy data with intraday market feeds.

## Intraday Data Collection

Intraday minute bars are sourced from the configured FivePaisa market-feed API via `FivePaisaService`. The scheduler requests market snapshots for the previously shortlisted Day-1 BTST symbols every three minutes during market hours and persists them through `IntradayDataService`.

The service gracefully falls back to zeroed placeholders when the sandbox cannot reach the upstream API, so local development and tests can still execute without live market connectivity.

### FivePaisa Authentication

The backend now mirrors the official **TOTP login flow** exposed by the FivePaisa Java SDK. Supply your credentials through environment variables or `application.yml`:

| Property | Purpose |
| --- | --- |
| `fivepaisa.api.app-name` / `fivepaisa.api.app-version` | Vendor app identifier issued by FivePaisa |
| `fivepaisa.api.user-key` / `fivepaisa.api.encrypt-key` | API keys from the developer console |
| `fivepaisa.api.user-id` / `fivepaisa.api.password` | Back-office credentials tied to the vendor app |
| `fivepaisa.api.login-id` / `fivepaisa.api.client-code` | Trading login (client code) |
| `fivepaisa.api.pin` | 2FA PIN required for order placement |
| `fivepaisa.api.totp-secret` *(preferred)* | Base32 seed used to generate 30-second TOTPs |
| `fivepaisa.api.totp-code` *(fallback)* | Manually supplied TOTP code if a secret is unavailable |
| `fivepaisa.api.device-ip` / `fivepaisa.api.device-id` *(optional)* | Override the default device fingerprint |

At startup `FivePaisaService` builds an `AppConfig`, derives the current TOTP (from the secret when provided), and calls `RestClient#getTotpSession`. The response contains the `AccessToken`, `RefreshToken`, and `FeedToken`, which are cached until expiry. When the sandbox cannot reach the upstream API the service logs the failure and continues with zeroed market data, keeping the rest of the pipeline operational.

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

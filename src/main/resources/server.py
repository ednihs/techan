import os
import httpx
from fastmcp import FastMCP

DATA_BASE = os.getenv("DATA_BASE", "http://127.0.0.1:8081").rstrip("/")
DATA_TOKEN = os.getenv("DATA_TOKEN")

mcp = FastMCP("data-api")

def _headers():
    h = {}
    if DATA_TOKEN:
        h["Authorization"] = f"Bearer {DATA_TOKEN}"
    return h

@mcp.tool()
async def fetch_indicators():
    """
    Proxy: GET {DATA_BASE}/api/v1/analysis/indicators/all
    Returns upstream JSON verbatim.
    """
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.get(f"{DATA_BASE}/api/v1/analysis/indicators/all", headers=_headers())
        r.raise_for_status()
        return r.json()

@mcp.tool()
async def fetch_prices(symbol: str):
    """
    Proxy: GET {DATA_BASE}/api/v1/analysis/prices/{symbol}
    Returns upstream JSON verbatim.
    """
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.get(f"{DATA_BASE}/api/v1/analysis/prices/{symbol}", headers=_headers())
        r.raise_for_status()
        return r.json()

@mcp.tool()
async def fetch_holdings():
    """
    Proxy: GET {DATA_BASE}/api/portfolio/holdings
    Returns upstream JSON verbatim.
    """
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.get(f"{DATA_BASE}/api/portfolio/holdings", headers=_headers())
        r.raise_for_status()
        return r.json()

@mcp.tool()
async def place_order(symbol: str, exchange: str, order_type: str, transaction_type: str, quantity: int, price: float, product_type: str):
    """
    Proxy: POST {DATA_BASE}/api/portfolio/order
    Returns upstream JSON verbatim.
    """
    order_request = {
        "symbol": symbol,
        "exchange": exchange,
        "orderType": order_type,
        "transactionType": transaction_type,
        "quantity": quantity,
        "price": price,
        "productType": product_type
    }
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.post(f"{DATA_BASE}/api/portfolio/order", headers=_headers(), json=order_request)
        r.raise_for_status()
        return r.json()

@mcp.tool()
async def cancel_order(order_id: str):
    """
    Proxy: DELETE {DATA_BASE}/api/portfolio/order/{order_id}
    Returns upstream JSON verbatim.
    """
    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.delete(f"{DATA_BASE}/api/portfolio/order/{order_id}", headers=_headers())
        r.raise_for_status()
        return r.json()

@mcp.tool()
async def fetch_enriched_indicator(symbol: str, date: str = None):
    """
    Proxy: GET {DATA_BASE}/api/v1/analysis/indicators/{symbol}
    Returns upstream JSON verbatim.
    """
    params = {}
    if date:
        params["date"] = date

    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.get(f"{DATA_BASE}/api/v1/analysis/indicators/{symbol}", headers=_headers(), params=params)
        r.raise_for_status()
        return r.json()
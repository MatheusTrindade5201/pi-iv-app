import json, subprocess, sys
from datetime import UTC, datetime, timedelta
from pathlib import Path
import httpx

OUT = Path(sys.argv[1])
B = "http://localhost:8776/api/v1"
EMAIL = "fixture.app@example.com"
PW = "senha-segura-123"
GAS = "00000000-0000-4000-8000-000000000001"
c = httpx.Client(base_url=B, timeout=30)
H = {}

def psql(sql):
    subprocess.run(["docker", "exec", "pi-iv-db-1", "psql", "-U", "pi_iv", "-qAt", "-c", sql], check=True, capture_output=True)

def save(name, r, expect):
    assert r.status_code == expect, (name, r.status_code, r.text)
    body = r.json()
    if isinstance(body, dict) and "access_token" in body:
        body["access_token"] = "token.ficticio.de.teste"
    (OUT / f"{name}.json").write_text(json.dumps(body, ensure_ascii=False, indent=2) + "\n")
    return body

save("health", c.get("/health"), 200)
save("register", c.post("/auth/register", json={"name": "John Doe", "email": EMAIL, "password": PW, "password_confirm": PW}), 201)
save("error_conflict", c.post("/auth/register", json={"name": "John Doe", "email": EMAIL, "password": PW, "password_confirm": PW}), 409)
save("error_validation", c.post("/auth/register", json={"name": "", "email": "nao-e-email", "password": "x", "password_confirm": "y"}), 422)
save("error_unauthorized", c.post("/auth/login", json={"email": EMAIL, "password": "errada-123"}), 401)
tok = c.post("/auth/login", json={"email": EMAIL, "password": PW}).json()["access_token"]
save("login", c.post("/auth/login", json={"email": EMAIL, "password": PW}), 200)
H["Authorization"] = f"Bearer {tok}"
c.headers.update(H)
save("users_me", c.get("/users/me"), 200)
save("users_me_update", c.patch("/users/me", json={"name": "John Doe"}), 200)
save("fuels", c.get("/fuels"), 200)
save("fuel_prices_update", c.put("/fuel-prices", json={"prices": [{"fuel_id": GAS, "price_per_liter": "6.20"}, {"fuel_id": "00000000-0000-4000-8000-000000000002", "price_per_liter": "3.75"}]}), 200)
save("fuel_prices_current", c.get("/fuel-prices/current"), 200)
v = save("vehicle", c.post("/vehicles", json={"model": "Honda CG 160", "fuel_id": GAS, "declared_kml": "32", "displacement_cc": 160, "year": 2022}), 201)
save("vehicles", c.get("/vehicles"), 200)
save("vehicle_update", c.patch(f"/vehicles/{v['id']}", json={"year": None}), 200)
save("error_not_found", c.get("/vehicles/00000000-0000-4000-8000-0000000000ff"), 404)
s = save("shift_started", c.post("/shifts", json={"vehicle_id": v["id"]}), 201)
started = datetime.now(UTC) - timedelta(hours=4)
psql(f"update shifts set started_at = '{started.isoformat()}' where id = '{s['id']}'")
pts = [(5, -23.550520, -46.633308), (20, -23.561414, -46.655881), (30, -23.561420, -46.655890), (60, -23.587416, -46.657634), (70, -23.587420, -46.657640)]
batch = {"points": [{"recorded_at": (started + timedelta(minutes=m)).isoformat(), "latitude": la, "longitude": lo, "accuracy_m": 8.5, "speed_ms": 9.1} for m, la, lo in pts]}
save("track_points_send", c.post(f"/shifts/{s['id']}/track-points", json=batch), 202)
save("track_points", c.get(f"/shifts/{s['id']}/track-points"), 200)
e = save("earning", c.post(f"/shifts/{s['id']}/earnings", json={"amount": "20", "platform": "iFood"}), 201)
c.post(f"/shifts/{s['id']}/earnings", json={"amount": "12"})
save("earning_update", c.patch(f"/shifts/{s['id']}/earnings/{e['id']}", json={"amount": "8"}), 200)
save("earnings", c.get(f"/shifts/{s['id']}/earnings"), 200)
save("shift_current", c.get("/shifts/current"), 200)
save("shift_finished", c.post(f"/shifts/{s['id']}/finish"), 200)
save("error_shift_finished", c.post(f"/shifts/{s['id']}/finish"), 409)
save("shift_detail", c.get(f"/shifts/{s['id']}"), 200)
save("shifts_page", c.get("/shifts", params={"size": 10}), 200)
save("analytics_daily", c.get("/analytics/daily"), 200)
save("analytics_summary", c.get("/analytics/summary"), 200)
c.headers.pop("Authorization")
save("error_unauthenticated", c.get("/users/me"), 401)
print("ok", len(list(OUT.glob("*.json"))), "fixtures")

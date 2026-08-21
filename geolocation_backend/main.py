# pyrefly: ignore [missing-import]
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from app.geo.geo_service import get_ip_location
import os
from dotenv import load_dotenv

# Load env variables (for MaxMind credentials if used later)
load_dotenv()

app = FastAPI(title="Blocked IP Geolocation Module")

# Define paths
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.join(BASE_DIR, "static")
TEMPLATES_DIR = os.path.join(BASE_DIR, "templates")

# Mount static files and templates
os.makedirs(STATIC_DIR, exist_ok=True)
os.makedirs(os.path.join(STATIC_DIR, "js"), exist_ok=True)
os.makedirs(TEMPLATES_DIR, exist_ok=True)

app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")
templates = Jinja2Templates(directory=TEMPLATES_DIR)

@app.get("/api/geolocation/{ip}")
def get_geolocation_api(ip: str):
    """
    Returns structured JSON with IP location data.
    """
    return get_ip_location(ip)

@app.get("/map/{ip}", response_class=HTMLResponse)
def view_map(request: Request, ip: str):
    """
    Renders the Leaflet.js map for the given IP address.
    """
    return templates.TemplateResponse("geolocation.html", {"request": request, "ip": ip})

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

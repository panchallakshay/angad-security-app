import os
import requests
import geoip2.database
from geoip2.errors import AddressNotFoundError

DB_PATH = os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'GeoLite2-City.mmdb')

def lookup_online_fallback(ip: str) -> dict:
    """Fallback to free ip-api for Cloudflare/Anycast/CDN IPs that have no coordinates in MaxMind."""
    try:
        url = f"http://ip-api.com/json/{ip}?fields=status,message,country,regionName,city,lat,lon"
        resp = requests.get(url, timeout=3.5)
        if resp.status_code == 200:
            data = resp.json()
            if data.get("status") == "success":
                return {
                    "country": data.get("country"),
                    "region": data.get("regionName"),
                    "city": data.get("city"),
                    "latitude": data.get("lat"),
                    "longitude": data.get("lon"),
                    "accuracy_radius_km": 50,
                    "source": "ip-api fallback"
                }
    except Exception as e:
        print(f"Online fallback failed: {e}")
    return {"error": "Location data not found in database or online resolvers"}

def lookup_ip_geolite(ip: str) -> dict:
    result = None
    if os.path.exists(DB_PATH):
        try:
            with geoip2.database.Reader(DB_PATH) as reader:
                response = reader.city(ip)
                
                lat = response.location.latitude
                lon = response.location.longitude
                
                # If MaxMind gave us coordinates, use it
                if lat is not None and lon is not None:
                    return {
                        "country": response.country.name,
                        "region": response.subdivisions.most_specific.name,
                        "city": response.city.name,
                        "latitude": lat,
                        "longitude": lon,
                        "accuracy_radius_km": response.location.accuracy_radius or 25,
                        "source": "GeoLite2"
                    }
        except (AddressNotFoundError, Exception) as e:
            print(f"GeoLite2 lookup note: {e}")
    
    # If MaxMind failed or coordinates were null (e.g. Cloudflare/Anycast IP), use fast fallback
    return lookup_online_fallback(ip)

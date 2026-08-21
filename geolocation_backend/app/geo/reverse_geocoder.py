import requests
import time

def reverse_geocode(lat: float, lon: float) -> str:
    """
    Uses Nominatim to reverse geocode latitude and longitude.
    """
    if lat is None or lon is None:
        return None
        
    url = "https://nominatim.openstreetmap.org/reverse"
    params = {
        "lat": lat,
        "lon": lon,
        "format": "json"
    }
    
    headers = {
        "User-Agent": "AngadSecurityApp-IPGeolocationModule/1.0"
    }
    
    try:
        response = requests.get(url, params=params, headers=headers, timeout=5)
        if response.status_code == 200:
            data = response.json()
            return data.get("display_name", "Approximate location based on geocoded coordinates")
        else:
            print(f"Nominatim API Error: {response.status_code}")
            return "Approximate location based on geocoded coordinates"
    except Exception as e:
        print(f"Reverse Geocoding failed: {e}")
        return "Approximate location based on geocoded coordinates"

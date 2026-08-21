from app.geo.ip_validator import validate_ip
from app.geo.cache import get_cached_location, save_to_cache
from app.geo.geo_lookup import lookup_ip_geolite
from app.geo.reverse_geocoder import reverse_geocode

def get_ip_location(ip: str) -> dict:
    """
    Core orchestration function to get IP location.
    """
    # 1. Validate IP
    validation_result = validate_ip(ip)
    if not validation_result["valid"]:
        return {
            "success": False,
            "ip": ip,
            "ip_type": validation_result["ip_type"],
            "error": validation_result["error"]
        }
        
    # 2. Check Cache
    cached_data = get_cached_location(ip)
    if cached_data:
        cached_data["metadata"]["source"] += " (Cached)"
        return cached_data
        
    # 3. GeoLite2 Lookup
    geo_data = lookup_ip_geolite(ip)
    if "error" in geo_data:
        return {
            "success": False,
            "ip": ip,
            "ip_type": "public",
            "error": geo_data["error"]
        }
        
    # 4. Reverse Geocode (if coordinates available)
    display_address = None
    if geo_data.get("latitude") and geo_data.get("longitude"):
        display_address = reverse_geocode(geo_data["latitude"], geo_data["longitude"])
        
    # 5. Construct Final JSON
    result = {
        "success": True,
        "ip": ip,
        "ip_type": "public",
        "location": {
            "country": geo_data.get("country"),
            "region": geo_data.get("region"),
            "city": geo_data.get("city"),
            "latitude": geo_data.get("latitude"),
            "longitude": geo_data.get("longitude"),
            "accuracy_radius_km": geo_data.get("accuracy_radius_km")
        },
        "address": {
            "display_name": display_address or "Approximate location based on geocoded coordinates"
        },
        "map": {
            "latitude": geo_data.get("latitude"),
            "longitude": geo_data.get("longitude")
        },
        "metadata": {
            "source": "GeoLite2",
            "location_type": "approximate_ip_geolocation"
        }
    }
    
    # Save to Cache
    save_to_cache(ip, result)
    
    return result

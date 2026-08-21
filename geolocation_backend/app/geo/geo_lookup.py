import os
import geoip2.database
from geoip2.errors import AddressNotFoundError

DB_PATH = os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'GeoLite2-City.mmdb')

def lookup_ip_geolite(ip: str) -> dict:
    if not os.path.exists(DB_PATH):
        return {"error": "GeoLite2 database not found. Please download it and place it in the data/ folder."}
        
    try:
        with geoip2.database.Reader(DB_PATH) as reader:
            response = reader.city(ip)
            
            # Extract safely
            country = response.country.name
            region = response.subdivisions.most_specific.name
            city = response.city.name
            latitude = response.location.latitude
            longitude = response.location.longitude
            accuracy_radius = response.location.accuracy_radius
            
            return {
                "country": country,
                "region": region,
                "city": city,
                "latitude": latitude,
                "longitude": longitude,
                "accuracy_radius_km": accuracy_radius
            }
    except AddressNotFoundError:
        return {"error": "Address not found in GeoLite2 database"}
    except Exception as e:
        return {"error": str(e)}

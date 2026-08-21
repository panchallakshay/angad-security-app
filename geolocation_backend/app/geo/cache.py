import sqlite3
import json
from datetime import datetime, timedelta
import os

DB_PATH = os.path.join(os.path.dirname(__file__), '..', '..', 'database', 'ip_geolocation_cache.db')

def init_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ip_geolocation_cache (
            ip_address TEXT PRIMARY KEY,
            data_json TEXT,
            last_updated DATETIME
        )
    ''')
    conn.commit()
    conn.close()

def get_cached_location(ip: str, expiry_days: int = 7):
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT data_json, last_updated FROM ip_geolocation_cache WHERE ip_address = ?", (ip,))
        row = cursor.fetchone()
        conn.close()
        
        if row:
            data_json, last_updated_str = row
            last_updated = datetime.fromisoformat(last_updated_str)
            if datetime.now() - last_updated < timedelta(days=expiry_days):
                return json.loads(data_json)
    except Exception as e:
        print(f"Cache read error: {e}")
    return None

def save_to_cache(ip: str, location_data: dict):
    try:
        init_db()
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        data_json = json.dumps(location_data)
        last_updated = datetime.now().isoformat()
        cursor.execute('''
            INSERT OR REPLACE INTO ip_geolocation_cache (ip_address, data_json, last_updated)
            VALUES (?, ?, ?)
        ''', (ip, data_json, last_updated))
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"Cache write error: {e}")

# Initialize db on module import
init_db()

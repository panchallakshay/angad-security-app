document.addEventListener('DOMContentLoaded', () => {
    fetchLocationData(targetIp);
});

function fetchLocationData(ip) {
    fetch(`/api/geolocation/${ip}`)
        .then(response => response.json())
        .then(data => {
            const loader = document.getElementById('loader');
            const infoContent = document.getElementById('info-content');
            if (loader) loader.style.display = 'none';
            if (infoContent) infoContent.style.display = 'block';
            
            const ipBadge = document.getElementById('val-ip');
            if (ipBadge) ipBadge.textContent = data.ip || ip;

            if (data.success && data.location && data.location.latitude && data.location.longitude) {
                const loc = data.location;
                const locStr = [loc.city, loc.region, loc.country].filter(Boolean).join(', ') || 'Unknown Region';
                
                const valLoc = document.getElementById('val-location');
                const valCoords = document.getElementById('val-coords');
                const valAcc = document.getElementById('val-accuracy');
                
                if (valLoc) valLoc.textContent = locStr;
                if (valCoords) valCoords.textContent = `${Number(loc.latitude).toFixed(4)}, ${Number(loc.longitude).toFixed(4)}`;
                if (valAcc) valAcc.textContent = loc.accuracy_radius_km ? `~${loc.accuracy_radius_km} km` : 'Standard';
                
                initMap(loc.latitude, loc.longitude, data);
            } else {
                const errorBox = document.getElementById('error-box');
                const geoDetails = document.getElementById('geo-details');
                if (geoDetails) geoDetails.style.display = 'none';
                if (errorBox) {
                    errorBox.style.display = 'block';
                    errorBox.textContent = data.error || 'Location data not available for this IP.';
                }
                initMap(20.5937, 78.9629, null, true); // Default world/center view
            }
        })
        .catch(err => {
            console.error('Error fetching geolocation:', err);
            const loader = document.getElementById('loader');
            if (loader) loader.textContent = 'Failed to load geolocation data.';
            initMap(20.5937, 78.9629, null, true);
        });
}

function initMap(lat, lon, data, isError = false) {
    try {
        const mapContainer = document.getElementById('map');
        if (!mapContainer) return;

        // Clear existing map instance if any
        if (window.currentMap) {
            window.currentMap.remove();
        }

        const map = L.map('map', {
            zoomControl: false,
            attributionControl: false
        }).setView([lat, lon], isError ? 3 : 11);

        window.currentMap = map;

        // Dark Matter tiles for cybersecurity aesthetics & high reliability
        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            maxZoom: 19,
            subdomains: 'abcd'
        }).addTo(map);

        if (!isError && data && data.location) {
            // Custom red pin marker
            const redIcon = L.divIcon({
                className: 'custom-div-icon',
                html: "<div style='background-color:#ff1744;width:16px;height:16px;border-radius:50%;border:3px solid #ffffff;box-shadow:0 0 10px #ff1744;'></div>",
                iconSize: [16, 16],
                iconAnchor: [8, 8]
            });

            const marker = L.marker([lat, lon], { icon: redIcon }).addTo(map);
            
            const city = data.location.city || 'Unknown';
            const country = data.location.country || '';
            const popupContent = `
                <div style="font-family:sans-serif;font-size:12px;padding:4px;">
                    <b style="color:#d4af37;">${city}</b>, ${country}<br>
                    <span style="color:#888;font-size:10px;">IP: ${data.ip}</span>
                </div>
            `;
            marker.bindPopup(popupContent).openPopup();
            
            if (data.location.accuracy_radius_km) {
                L.circle([lat, lon], {
                    color: '#ff1744',
                    fillColor: '#ff1744',
                    fillOpacity: 0.15,
                    weight: 1,
                    radius: data.location.accuracy_radius_km * 1000
                }).addTo(map);
            }
        }

        // Multiple invalidateSize calls to ensure map renders after WebView finishes layout calculation
        setTimeout(() => map.invalidateSize(), 100);
        setTimeout(() => map.invalidateSize(), 300);
        setTimeout(() => map.invalidateSize(), 800);

    } catch (e) {
        console.error('Error rendering Leaflet map:', e);
    }
}

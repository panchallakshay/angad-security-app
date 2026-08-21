document.addEventListener('DOMContentLoaded', () => {
    fetchLocationData(targetIp);
});

function fetchLocationData(ip) {
    fetch(`/api/geolocation/${ip}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('loader').style.display = 'none';
            document.getElementById('info-content').style.display = 'block';
            
            document.getElementById('val-ip').textContent = data.ip;
            document.getElementById('val-type').textContent = data.ip_type;

            if (data.success && data.location && data.location.latitude && data.location.longitude) {
                document.getElementById('geo-details').style.display = 'block';
                
                const loc = data.location;
                document.getElementById('val-city').textContent = loc.city || 'Unknown City';
                document.getElementById('val-region').textContent = loc.region || 'Unknown Region';
                document.getElementById('val-country').textContent = loc.country || 'Unknown Country';
                document.getElementById('val-coords').textContent = `${loc.latitude}, ${loc.longitude}`;
                document.getElementById('val-address').textContent = data.address.display_name;
                
                initMap(loc.latitude, loc.longitude, data);
            } else {
                document.getElementById('error-box').style.display = 'block';
                document.getElementById('error-box').textContent = data.error || 'Location data not available.';
                // Show a default map view if no coords
                initMap(0, 0, null, true);
            }
        })
        .catch(err => {
            console.error('Error fetching geolocation:', err);
            document.getElementById('loader').textContent = 'Failed to load data.';
        });
}

function initMap(lat, lon, data, isError = false) {
    const map = L.map('map').setView([lat, lon], isError ? 2 : 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    if (!isError && data) {
        const marker = L.marker([lat, lon]).addTo(map);
        
        const popupContent = `
            <div style="font-family: sans-serif;">
                <h3 style="margin:0 0 5px 0; color:#d32f2f;">Blocked IP</h3>
                <b>IP:</b> ${data.ip}<br>
                <b>City:</b> ${data.location.city || 'N/A'}<br>
                <b>Region:</b> ${data.location.region || 'N/A'}<br>
                <b>Country:</b> ${data.location.country || 'N/A'}<br>
                <hr style="margin:5px 0; border:0; border-top:1px solid #ccc;">
                <i style="font-size: 11px;">Approximate IP location</i>
            </div>
        `;
        
        marker.bindPopup(popupContent).openPopup();
        
        if (data.location.accuracy_radius_km) {
            L.circle([lat, lon], {
                color: 'red',
                fillColor: '#f03',
                fillOpacity: 0.2,
                radius: data.location.accuracy_radius_km * 1000 // Convert km to meters
            }).addTo(map);
        }
    }
}

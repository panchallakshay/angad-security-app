import ipaddress

def validate_ip(ip_str: str) -> dict:
    """
    Validates an IP address and determines if it is global (public) or private/special.
    Returns a dictionary with success status, error messages, and ip_type.
    """
    try:
        ip = ipaddress.ip_address(ip_str)
        
        if ip.is_loopback:
            return {"valid": False, "error": "Loopback IP cannot be geolocated", "ip_type": "loopback"}
        if ip.is_multicast:
            return {"valid": False, "error": "Multicast IP cannot be geolocated", "ip_type": "multicast"}
        if ip.is_unspecified:
            return {"valid": False, "error": "Unspecified IP cannot be geolocated", "ip_type": "unspecified"}
        if ip.is_reserved:
            return {"valid": False, "error": "Reserved IP cannot be geolocated", "ip_type": "reserved"}
        if ip.is_private:
            return {"valid": False, "error": "Private/local IP cannot be globally geolocated", "ip_type": "private"}
            
        # If it passes all above, it's a valid public IP
        return {"valid": True, "error": None, "ip_type": "public"}
        
    except ValueError:
        return {"valid": False, "error": "Invalid IP address syntax", "ip_type": "invalid"}

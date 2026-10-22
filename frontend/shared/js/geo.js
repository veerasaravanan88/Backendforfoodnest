// High-precision geocoding and location preset resolver for AdvanceFoodnest
// Specializes in South Indian towns (Madurai, T.Kallupatti, Chennai, etc.) and Bengaluru.

export const LOCATION_PRESETS = [
  // Madurai District Suburbs & Towns
  { match: /t\.?\s*kallup+at+i|kallup+at+i|peraiyur|625702|625703/i, lat: 9.7206, lng: 77.8517, label: 'T.Kallupatti, Madurai (625702)' },
  { match: /thirumangalam|tirumangalam|625706/i, lat: 9.8242, lng: 77.9897, label: 'Thirumangalam, Madurai' },
  { match: /usilampatti|625532/i, lat: 9.9678, lng: 77.7942, label: 'Usilampatti, Madurai' },
  { match: /sedapatti/i, lat: 9.8000, lng: 77.7800, label: 'Sedapatti, Madurai' },
  { match: /sholavandan|solavandan|625214/i, lat: 10.0215, lng: 78.0064, label: 'Sholavandan, Madurai' },
  { match: /vadipatti|625218/i, lat: 10.0833, lng: 77.9833, label: 'Vadipatti, Madurai' },
  { match: /melur|625106/i, lat: 10.0333, lng: 78.3333, label: 'Melur, Madurai' },
  { match: /alanganallur/i, lat: 10.0438, lng: 78.0934, label: 'Alanganallur, Madurai' },

  // Madurai City Localities
  { match: /goripalayam|goripalaayam/i, lat: 9.9288, lng: 78.1311, label: 'Goripalayam, Madurai' },
  { match: /simmakkal/i, lat: 9.9280, lng: 78.1215, label: 'Simmakkal, Madurai' },
  { match: /mattuthavani|matuthavani/i, lat: 9.9408, lng: 78.1565, label: 'Mattuthavani, Madurai' },
  { match: /anna\s*nagar.*madur|anna\s*nagar/i, lat: 9.9198, lng: 78.1487, label: 'Anna Nagar, Madurai' },
  { match: /kk\s*nagar|k\.k\.\s*nagar/i, lat: 9.9328, lng: 78.1534, label: 'KK Nagar, Madurai' },
  { match: /teppakulam/i, lat: 9.9135, lng: 78.1480, label: 'Teppakulam, Madurai' },
  { match: /villapuram/i, lat: 9.9015, lng: 78.1255, label: 'Villapuram, Madurai' },
  { match: /palanganatham/i, lat: 9.9082, lng: 78.0988, label: 'Palanganatham, Madurai' },
  { match: /sellur/i, lat: 9.9392, lng: 78.1189, label: 'Sellur, Madurai' },
  { match: /arappalayam/i, lat: 9.9332, lng: 78.1065, label: 'Arappalayam, Madurai' },
  { match: /periyar.*bus|periyar\s*madur/i, lat: 9.9175, lng: 78.1147, label: 'Periyar, Madurai' },
  { match: /thirunagar/i, lat: 9.8795, lng: 78.0583, label: 'Thirunagar, Madurai' },
  { match: /madurai|maduari/i, lat: 9.9252, lng: 78.1198, label: 'Madurai City Center' },

  // Madurai Pincode Wildcard (625xxx or 62502 typo)
  { match: /625\d{3}|62502/i, lat: 9.9252, lng: 78.1198, label: 'Madurai District (625xxx)' },

  // Other Tamil Nadu Hubs
  { match: /dindigul|624\d{3}/i, lat: 10.3673, lng: 77.9803, label: 'Dindigul' },
  { match: /virudhunagar|626\d{3}/i, lat: 9.5872, lng: 77.9514, label: 'Virudhunagar' },
  { match: /tirunelveli|nellai|627\d{3}/i, lat: 8.7139, lng: 77.7567, label: 'Tirunelveli' },
  { match: /thoothukudi|tuticorin|628\d{3}/i, lat: 8.7642, lng: 78.1348, label: 'Thoothukudi' },
  { match: /trichy|tiruchirappalli|620\d{3}|621\d{3}/i, lat: 10.7905, lng: 78.7047, label: 'Trichy' },
  { match: /salem|636\d{3}/i, lat: 11.6643, lng: 78.1460, label: 'Salem' },
  { match: /coimbatore|kovai|641\d{3}/i, lat: 11.0168, lng: 76.9558, label: 'Coimbatore' },
  { match: /chennai|t\s*nagar|600\d{3}/i, lat: 13.0827, lng: 80.2707, label: 'Chennai' },

  // Bengaluru Localities
  { match: /mg\s*road/i, lat: 12.9756, lng: 77.6066, label: 'MG Road, Bengaluru' },
  { match: /brigade/i, lat: 12.9740, lng: 77.6075, label: 'Brigade Road, Bengaluru' },
  { match: /residency/i, lat: 12.9716, lng: 77.6085, label: 'Residency Road, Bengaluru' },
  { match: /whitefield/i, lat: 12.9698, lng: 77.7499, label: 'Whitefield, Bengaluru' },
  { match: /indiranagar/i, lat: 12.9784, lng: 77.6408, label: 'Indiranagar, Bengaluru' },
  { match: /koramangala/i, lat: 12.9352, lng: 77.6245, label: 'Koramangala, Bengaluru' },
  { match: /hsr/i, lat: 12.9121, lng: 77.6446, label: 'HSR Layout, Bengaluru' },
  { match: /btm/i, lat: 12.9166, lng: 77.6101, label: 'BTM Layout, Bengaluru' },
  { match: /jayanagar/i, lat: 12.9308, lng: 77.5838, label: 'Jayanagar, Bengaluru' },
  { match: /electronic\s*city/i, lat: 12.8452, lng: 77.6602, label: 'Electronic City, Bengaluru' },
  { match: /bengaluru|bangalore|560\d{3}/i, lat: 12.9716, lng: 77.5946, label: 'Bengaluru City Center' }
];

/**
 * Geocodes an address string to { lat, lng, label }.
 * Prioritizes high-speed presets, then cleaned Nominatim queries, and context-aware fallbacks.
 *
 * @param {string} addressText The address to geocode
 * @param {string} contextAddress Optional restaurant/anchor address to prevent jumping across states
 */
export async function geocodeAddress(addressText, contextAddress = null) {
  if (!addressText) {
    return resolveContextFallback(contextAddress);
  }

  let cleaned = addressText.toLowerCase().trim();
  // Normalize common phonetic variations and typos
  cleaned = cleaned.replace(/maduari/g, 'madurai');
  cleaned = cleaned.replace(/t\.?\s*kallup+at+i/g, 't.kallupatti');
  cleaned = cleaned.replace(/kallup+at+i/g, 'kallupatti');

  // 1. Instant preset dictionary lookup
  for (const preset of LOCATION_PRESETS) {
    if (preset.match.test(cleaned)) {
      return { lat: preset.lat, lng: preset.lng, label: preset.label };
    }
  }

  // 2. OpenStreetMap Nominatim with retry strategies
  // Attempt A: Direct cleaned query
  try {
    const directResult = await queryNominatim(cleaned);
    if (directResult) return directResult;
  } catch (e) {
    console.debug('Nominatim direct lookup failed:', e);
  }

  // Attempt B: Extract town/district/pincode by stripping local street noise (e.g. "mela street", "door no 12")
  const stripped = cleaned
    .replace(/\b(mela|keela|north|south|east|west|periya|chinna)\s*(street|theru|st)\b/gi, '')
    .replace(/\b(door\s*no|d\.no|plot\s*no|flat\s*no|main\s*road|cross|lane|near|opp|opposite)\b[^\s,]*/gi, '')
    .replace(/[,\.\-\/]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  if (stripped && stripped !== cleaned) {
    // Check presets against stripped text
    for (const preset of LOCATION_PRESETS) {
      if (preset.match.test(stripped)) {
        return { lat: preset.lat, lng: preset.lng, label: preset.label };
      }
    }

    try {
      const strippedResult = await queryNominatim(stripped);
      if (strippedResult) return strippedResult;
    } catch (e) {
      console.debug('Nominatim stripped lookup failed:', e);
    }
  }

  // Attempt C: Extract 6-digit Indian PIN code if present
  const pinMatch = cleaned.match(/\b(6\d{5}|5\d{5})\b/);
  if (pinMatch) {
    const pin = pinMatch[1];
    if (pin.startsWith('625702') || pin.startsWith('625703')) {
      return { lat: 9.7206, lng: 77.8517, label: `T.Kallupatti (${pin})` };
    }
    if (pin.startsWith('625')) {
      return { lat: 9.9252, lng: 78.1198, label: `Madurai District (${pin})` };
    }
    try {
      const pinResult = await queryNominatim(`${pin}, India`);
      if (pinResult) return pinResult;
    } catch (e) {
      console.debug('Nominatim PIN lookup failed:', e);
    }
  }

  // 3. Contextual Regional Fallback (Never fall back to Bengaluru if the order is in Madurai / Tamil Nadu)
  return resolveContextFallback(cleaned, contextAddress);
}

async function queryNominatim(queryStr) {
  const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(queryStr)}&limit=1`;
  const resp = await fetch(url, { headers: { 'Accept-Language': 'en' } });
  if (resp.ok) {
    const data = await resp.json();
    if (data && data.length > 0) {
      return {
        lat: parseFloat(data[0].lat),
        lng: parseFloat(data[0].lon),
        label: data[0].display_name
      };
    }
  }
  return null;
}

function resolveContextFallback(primaryText = '', contextText = '') {
  const combined = `${primaryText} ${contextText}`.toLowerCase();

  if (/kallup+at+i|peraiyur|625702/i.test(combined)) {
    return { lat: 9.7206, lng: 77.8517, label: 'T.Kallupatti, Madurai' };
  }
  if (/madur|goripal|simmakkal|mattuthavani|625/i.test(combined)) {
    return { lat: 9.9252, lng: 78.1198, label: 'Madurai, Tamil Nadu' };
  }
  if (/chennai|600/i.test(combined)) {
    return { lat: 13.0827, lng: 80.2707, label: 'Chennai, Tamil Nadu' };
  }
  if (/coimbatore|kovai|641/i.test(combined)) {
    return { lat: 11.0168, lng: 76.9558, label: 'Coimbatore, Tamil Nadu' };
  }
  if (/trichy|620|621/i.test(combined)) {
    return { lat: 10.7905, lng: 78.7047, label: 'Trichy, Tamil Nadu' };
  }
  // Default to local town (T.Kallupatti / Madurai) instead of Bangalore
  return { lat: 9.7206, lng: 77.8517, label: 'T.Kallupatti, Madurai (625702)' };
}

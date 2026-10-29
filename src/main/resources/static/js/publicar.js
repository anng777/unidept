// Selector de ubicación: clic/arrastre, búsqueda de dirección y enlace de Google Maps.
(function () {
  var el = document.getElementById('mapa-picker');
  if (!el || !window.L) return;
  var latIn = document.getElementById('latitud'), lngIn = document.getElementById('longitud');
  var texto = document.getElementById('coord-texto'), info = document.getElementById('info-cercana');
  var unis = window.UNIVERSIDADES || [];
  var BOUNDS = { latMin: -12.55, latMax: -11.70, lngMin: -77.25, lngMax: -76.60 };

  function hav(a, b, c, d) {
    var R = 6371000, r = Math.PI / 180, dLa = (c - a) * r, dLo = (d - b) * r;
    var x = Math.sin(dLa / 2) ** 2 + Math.cos(a * r) * Math.cos(c * r) * Math.sin(dLo / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(x));
  }
  function dentro(la, ln) { return la >= BOUNDS.latMin && la <= BOUNDS.latMax && ln >= BOUNDS.lngMin && ln <= BOUNDS.lngMax; }

  var la0 = parseFloat(latIn.value), ln0 = parseFloat(lngIn.value);
  var tiene = !isNaN(la0) && !isNaN(ln0);
  var mapa = L.map(el).setView(tiene ? [la0, ln0] : [-12.0464, -77.0428], tiene ? 16 : 11);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
  var marcador = null;

  function fijar(la, ln, centrar) {
    if (!marcador) {
      marcador = L.marker([la, ln], { draggable: true }).addTo(mapa);
      marcador.on('dragend', function () { var p = marcador.getLatLng(); fijar(p.lat, p.lng, false); });
    } else marcador.setLatLng([la, ln]);
    latIn.value = la.toFixed(6); lngIn.value = ln.toFixed(6);
    texto.textContent = '(' + la.toFixed(5) + ', ' + ln.toFixed(5) + ')';
    if (centrar) mapa.setView([la, ln], 17);
    if (!dentro(la, ln)) { info.className = 'small mt-1 text-danger'; info.textContent = 'Ese punto está fuera de Lima y Callao.'; return; }
    var mejor = null;
    unis.forEach(function (u) {
      var d = hav(la, ln, u.latitud, u.longitud);
      if (!mejor || d < mejor.d) mejor = { d: d, n: u.nombre };
    });
    if (mejor) {
      info.className = 'small mt-1 text-muted';
      info.textContent = 'Universidad más cercana: ' + mejor.n + ' (' + (mejor.d < 1000 ? Math.round(mejor.d) + ' m' : (mejor.d / 1000).toFixed(1) + ' km') + ' en línea recta).';
    }
  }
  if (tiene) fijar(la0, ln0, false);
  mapa.on('click', function (e) { fijar(e.latlng.lat, e.latlng.lng, false); });

  document.getElementById('btn-buscar').addEventListener('click', function () {
    var q = [document.getElementById('direccion').value, document.getElementById('distrito').value, 'Lima, Perú'].join(', ');
    fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=pe&q=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.json(); })
      .then(function (d) {
        if (d && d.length) fijar(parseFloat(d[0].lat), parseFloat(d[0].lon), true);
        else alert('No se encontró la dirección. Haz clic en el mapa para marcar el punto exacto.');
      }).catch(function () { alert('No se pudo buscar la dirección. Marca el punto manualmente en el mapa.'); });
  });

  document.getElementById('btn-enlace').addEventListener('click', function () {
    var t = document.getElementById('enlace-maps').value;
    var m = t.match(/@(-?\d+\.\d+),(-?\d+\.\d+)/) || t.match(/!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)/) || t.match(/[?&](?:q|query|ll)=(-?\d+\.\d+),(-?\d+\.\d+)/);
    if (m) fijar(parseFloat(m[1]), parseFloat(m[2]), true);
    else alert('No pude leer coordenadas de ese enlace (los enlaces cortos maps.app.goo.gl no las incluyen). Usa el enlace largo o marca el punto en el mapa.');
  });

  document.getElementById('form-publicar').addEventListener('submit', function (e) {
    if (!latIn.value || !lngIn.value) { e.preventDefault(); alert('Marca la ubicación exacta en el mapa antes de guardar.'); }
  });
})();

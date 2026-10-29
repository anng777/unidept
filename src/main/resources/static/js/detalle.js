// Mapa (detalle) + calendario con fechas ocupadas en rojo suave (detalle y disponibilidad).
(function () {
  var raiz = document.getElementById('detalle') || document.getElementById('disp');
  if (!raiz) return;
  var id = raiz.getAttribute('data-id');

  /* ---------- Mapa: alojamiento + universidad ---------- */
  var mapaEl = document.getElementById('mapa');
  if (mapaEl && window.L) {
    var lat = parseFloat(raiz.getAttribute('data-lat')), lng = parseFloat(raiz.getAttribute('data-lng'));
    var ulat = parseFloat(mapaEl.getAttribute('data-uni-lat')), ulng = parseFloat(mapaEl.getAttribute('data-uni-lng'));
    if (!isNaN(lat) && !isNaN(lng)) {
      var mapa = L.map(mapaEl);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
      var casa = L.marker([lat, lng]).addTo(mapa).bindPopup(mapaEl.getAttribute('data-titulo') || 'Alojamiento');
      var puntos = [[lat, lng]];
      if (!isNaN(ulat) && !isNaN(ulng)) {
        L.marker([ulat, ulng], { icon: L.divIcon({ className: '', html: '<div style="font-size:26px">🎓</div>', iconSize: [28, 28], iconAnchor: [14, 14] }) })
          .addTo(mapa).bindPopup(mapaEl.getAttribute('data-uni-nombre') || 'Universidad');
        L.polyline([[lat, lng], [ulat, ulng]], { color: '#1d4ed8', dashArray: '6 6', weight: 2 }).addTo(mapa);
        puntos.push([ulat, ulng]);
      }
      mapa.fitBounds(puntos, { padding: [40, 40], maxZoom: 17 });
      casa.openPopup();
    }
  }

  /* ---------- Calendario ---------- */
  var calEl = document.getElementById('calendario');
  if (!calEl || !window.flatpickr) return;
  var form = document.getElementById('form-solicitud');
  var ocupadas = {};
  function iso(d) { return flatpickr.formatDate(d, 'Y-m-d'); }
  function cargar(cb) {
    fetch(window.UNI_BASE + 'api/alojamientos/' + id + '/ocupadas', { credentials: 'same-origin' })
      .then(function (r) { return r.json(); })
      .then(function (d) {
        (d.rangos || []).forEach(function (rg) {
          var cur = new Date(rg.desde + 'T00:00:00'), fin = new Date(rg.hasta + 'T00:00:00');
          while (cur <= fin) { ocupadas[iso(cur)] = true; cur.setDate(cur.getDate() + 1); }
        });
      }).catch(function () {}).then(cb);
  }
  function hayChoque(a, b) {
    var cur = new Date(a.getTime());
    while (cur <= b) { if (ocupadas[iso(cur)]) return true; cur.setDate(cur.getDate() + 1); }
    return false;
  }
  cargar(function () {
    var msg = document.getElementById('msg-fechas');
    var desde = document.getElementById('desde'), hasta = document.getElementById('hasta');
    var cfg = {
      inline: true,
      locale: 'es',
      dateFormat: 'Y-m-d',
      disable: [function (d) { return !!ocupadas[iso(d)]; }],
      onDayCreate: function (_, __, ___, dayElem) {
        if (ocupadas[iso(dayElem.dateObj)]) { dayElem.classList.add('ocupado'); dayElem.title = 'Ocupado'; }
      }
    };
    if (form) {
      cfg.mode = 'range';
      cfg.minDate = 'today';
      cfg.onChange = function (sel, _, fp) {
        msg.classList.add('d-none');
        if (sel.length === 2) {
          if (hayChoque(sel[0], sel[1])) {
            msg.textContent = 'Tu rango incluye fechas ocupadas (en rojo). Elige otras fechas.';
            msg.classList.remove('d-none'); fp.clear(); desde.value = ''; hasta.value = ''; return;
          }
          desde.value = iso(sel[0]); hasta.value = iso(sel[1]);
        } else { desde.value = ''; hasta.value = ''; }
      };
      form.addEventListener('submit', function (e) {
        if (!desde.value || !hasta.value) {
          e.preventDefault();
          msg.textContent = 'Selecciona en el calendario la fecha de inicio y la de fin.'; msg.classList.remove('d-none');
        }
      });
    } else {
      cfg.clickOpens = false;           // solo lectura (visitantes y arrendador)
      cfg.onChange = function (_, __, fp) { fp.clear(); };
    }
    flatpickr(calEl, cfg);
  });
})();

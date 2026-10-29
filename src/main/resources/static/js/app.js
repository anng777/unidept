// Notificaciones (campanita): consulta periódicamente mensajes sin leer y solicitudes pendientes.
(function () {
  var nav = document.querySelector('nav[data-auth="true"]');
  if (!nav) return;
  var url = nav.getAttribute('data-url');
  function pintar(id, n) {
    var el = document.getElementById(id);
    if (!el) return;
    el.textContent = n;
    el.classList.toggle('d-none', !(n > 0));
  }
  function consultar() {
    fetch(url, { credentials: 'same-origin' })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (d) { if (d) { pintar('badge-msg', d.mensajes); pintar('badge-sol', d.solicitudes); } })
      .catch(function () {});
  }
  consultar();
  setInterval(consultar, 20000);
})();

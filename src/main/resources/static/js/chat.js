(function () {
  var cont = document.getElementById('chat');
  if (!cont) return;
  var id = cont.getAttribute('data-id');
  var caja = document.getElementById('caja-chat');
  caja.scrollTop = caja.scrollHeight;
  var ultimo = null;
  setInterval(function () {
    fetch(window.UNI_BASE + 'chat/' + id + '/mensajes', { credentials: 'same-origin' })
      .then(function (r) { return r.ok ? r.text() : null; })
      .then(function (html) {
        if (!html) return;
        var actual = document.getElementById('lista-mensajes');
        var abajo = caja.scrollHeight - caja.scrollTop - caja.clientHeight < 40;
        if (actual && html !== ultimo) {
          ultimo = html;
          actual.outerHTML = html;
          if (abajo) caja.scrollTop = caja.scrollHeight;
        }
      }).catch(function () {});
  }, 5000);
})();

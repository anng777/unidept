// El resto del formulario solo se habilita cuando RENIEC confirma el DNI.
(function () {
  var dni = document.getElementById('dni'), btn = document.getElementById('btn-dni');
  var msg = document.getElementById('dni-msg'), datos = document.getElementById('datos');
  var nombre = document.getElementById('nombre-reniec');
  var ayuda = document.getElementById('ayuda-email');
  var verificado = null;

  function bloquear() { datos.disabled = true; nombre.value = ''; verificado = null; }
  dni.addEventListener('input', function () {
    dni.value = dni.value.replace(/\D/g, '').slice(0, 8);
    if (dni.value !== verificado) bloquear();
  });
  document.getElementById('telefono').addEventListener('input', function (e) {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 9);
  });
  document.querySelectorAll('input[name="tipoUsuario"]').forEach(function (r) {
    r.addEventListener('change', function () {
      ayuda.textContent = document.getElementById('t-est').checked
        ? 'Estudiantes: usa tu correo institucional (.edu.pe).' : 'Usa un correo que revises con frecuencia.';
    });
  });
  btn.addEventListener('click', function () {
    if (!/^\d{8}$/.test(dni.value)) { msg.textContent = 'El DNI debe tener 8 dígitos.'; msg.className = 'form-text text-danger'; return; }
    btn.disabled = true; msg.textContent = 'Consultando RENIEC…'; msg.className = 'form-text';
    fetch(window.UNI_BASE + 'api/dni?dni=' + encodeURIComponent(dni.value), { credentials: 'same-origin' })
      .then(function (r) { return r.json(); })
      .then(function (d) {
        if (d.ok) {
          verificado = dni.value; nombre.value = d.nombres + ' ' + d.apellidos;
          msg.textContent = 'DNI verificado.'; msg.className = 'form-text text-success'; datos.disabled = false;
        } else { msg.textContent = d.mensaje || 'No se pudo verificar el DNI.'; msg.className = 'form-text text-danger'; bloquear(); }
      })
      .catch(function () { msg.textContent = 'No se pudo consultar RENIEC. Intenta de nuevo.'; msg.className = 'form-text text-danger'; })
      .then(function () { btn.disabled = false; });
  });
})();

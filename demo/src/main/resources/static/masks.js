/* masks.js — Hystan input masks */

function maskCpfCnpj(e) {
  let v = e.target.value.replace(/\D/g, '');
  if (v.length > 14) v = v.substring(0, 14);
  if (v.length <= 11) {
    v = v.replace(/(\d{3})(\d)/, '$1.$2');
    v = v.replace(/(\d{3})(\d)/, '$1.$2');
    v = v.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  } else {
    v = v.replace(/(\d{2})(\d)/, '$1.$2');
    v = v.replace(/(\d{3})(\d)/, '$1.$2');
    v = v.replace(/(\d{3})(\d)/, '$1/$2');
    v = v.replace(/(\d{4})(\d{1,2})$/, '$1-$2');
  }
  e.target.value = v;
}

function maskTelefone(e) {
  let v = e.target.value.replace(/\D/g, '');
  if (v.length > 11) v = v.substring(0, 11);
  if (v.length <= 10) {
    v = v.replace(/(\d{2})(\d)/, '($1) $2');
    v = v.replace(/(\d{4})(\d)/, '$1-$2');
  } else {
    v = v.replace(/(\d{2})(\d)/, '($1) $2');
    v = v.replace(/(\d{5})(\d)/, '$1-$2');
  }
  e.target.value = v;
}

function maskNumeroRecibo(e) {
  e.target.value = e.target.value.replace(/\D/g, '').substring(0, 10);
}

function maskMoeda(e) {
  let v = e.target.value.replace(/\D/g, '');
  if (!v) { e.target.value = ''; return; }
  v = (parseInt(v, 10) / 100).toFixed(2);
  v = v.replace('.', ',');
  v = v.replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1.');
  e.target.value = 'R$ ' + v;
}

function maskCep(e) {
  let v = e.target.value.replace(/\D/g, '');
  if (v.length > 8) v = v.substring(0, 8);
  if (v.length > 5) v = v.replace(/^(\d{5})(\d{1,3})$/, '$1-$2');
  e.target.value = v;
}

function parseMoeda(v) {
  if (!v) return 0;
  return parseFloat(String(v).replace(/R\$\s*/g, '').replace(/\./g, '').replace(',', '.')) || 0;
}
